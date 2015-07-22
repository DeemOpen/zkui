/**
 *
 * Copyright (c) 2014, Deem Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package com.deem.zkui.utils;

import com.deem.zkui.vo.LeafBean;
import com.deem.zkui.vo.ZKNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.LoggerFactory;

public enum ZooKeeperUtil {

    INSTANCE;
    public final static Integer MAX_CONNECT_ATTEMPT = 5;
    public final static String ZK_ROOT_NODE = "/";
    public final static String ZK_SYSTEM_NODE = "zookeeper"; // ZK internal folder (quota info, etc) - have to stay away from it
    public final static String ZK_HOSTS = "/appconfig/hosts";
    public final static String ROLE_USER = "USER";
    public final static String ROLE_ADMIN = "ADMIN";
    public final static String SOPA_PIPA = "SOPA/PIPA BLACKLISTED VALUE";

    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(ZooKeeperUtil.class);

    public ZooKeeper createZKConnection(String url, Integer zkSessionTimeout) throws IOException, InterruptedException {
        Integer connectAttempt = 0;
        ZooKeeper zk = new ZooKeeper(url, zkSessionTimeout, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                logger.trace("Connecting to ZK.");
            }
        });
        //Wait till connection is established.
        while (zk.getState() != ZooKeeper.States.CONNECTED) {
            Thread.sleep(30);
            connectAttempt++;
            if (connectAttempt == MAX_CONNECT_ATTEMPT) {
                break;
            }
        }
        return zk;

    }

    private ArrayList<ACL> defaultAcl = ZooDefs.Ids.OPEN_ACL_UNSAFE;

    private ArrayList<ACL> defaultAcl() {
        return defaultAcl;
    }

    public void setDefaultAcl(String jsonAcl) {
        if (jsonAcl == null || jsonAcl.trim().length() == 0) {
            logger.trace("Using UNSAFE ACL. Anyone on your LAN can change your Zookeeper data");
            defaultAcl = ZooDefs.Ids.OPEN_ACL_UNSAFE;
            return;
        }
        // Don't let things happen in a half-baked state, build the new ACL and then set it into
        // defaultAcl
        ArrayList<ACL> newDefault = new ArrayList<>();
        try {
            JSONArray acls = (JSONArray) ((JSONObject) new JSONParser().parse(jsonAcl)).get("acls");
            for (Iterator it = acls.iterator(); it.hasNext();) {
                JSONObject acl = (JSONObject) it.next();
                String scheme = ((String) acl.get("scheme")).trim();
                String id = ((String) acl.get("id")).trim();
                int perms = 0;
                String permStr = ((String) acl.get("perms")).toLowerCase().trim();
                for (char c : permStr.toCharArray()) {
                    switch (c) {
                        case 'a':
                            perms += ZooDefs.Perms.ADMIN;
                            break;
                        case 'c':
                            perms += ZooDefs.Perms.CREATE;
                            break;
                        case 'd':
                            perms += ZooDefs.Perms.DELETE;
                            break;
                        case 'r':
                            perms += ZooDefs.Perms.READ;
                            break;
                        case 'w':
                            perms += ZooDefs.Perms.WRITE;
                            break;
                        case '*':
                            perms += ZooDefs.Perms.ALL;
                            break;
                        default:
                            throw new RuntimeException("Illegal permission character in ACL " + c);
                    }
                }
                newDefault.add(new ACL(perms, new Id(scheme, id)));
            }
        } catch (ParseException e) {
            // Throw it all the way up to the error handlers
            throw new RuntimeException("Unable to parse default ACL " + jsonAcl, e);
        }
        defaultAcl = newDefault;
    }

    public Set<LeafBean> searchTree(String searchString, ZooKeeper zk, String authRole) throws InterruptedException, KeeperException {
        //Export all nodes and then search.
        Set<LeafBean> searchResult = new TreeSet<>();
        Set<LeafBean> leaves = new TreeSet<>();
        exportTreeInternal(leaves, ZK_ROOT_NODE, zk, authRole);
        for (LeafBean leaf : leaves) {
            String leafValue = ServletUtil.INSTANCE.externalizeNodeValue(leaf.getValue());
            if (leaf.getPath().contains(searchString) || leaf.getName().contains(searchString) || leafValue.contains(searchString)) {
                searchResult.add(leaf);
            }
        }
        return searchResult;

    }

    public Set<LeafBean> exportTree(String zkPath, ZooKeeper zk, String authRole) throws InterruptedException, KeeperException {
        // 1. Collect nodes
        long startTime = System.currentTimeMillis();
        Set<LeafBean> leaves = new TreeSet<>();
        exportTreeInternal(leaves, zkPath, zk, authRole);
        long estimatedTime = System.currentTimeMillis() - startTime;
        logger.trace("Elapsed Time in Secs for Export: " + estimatedTime / 1000);
        return leaves;
    }

    private void exportTreeInternal(Set<LeafBean> entries, String path, ZooKeeper zk, String authRole) throws InterruptedException, KeeperException {
        // 1. List leaves
        entries.addAll(this.listLeaves(zk, path, authRole));
        // 2. Process folders
        for (String folder : this.listFolders(zk, path)) {
            exportTreeInternal(entries, this.getNodePath(path, folder), zk, authRole);
        }
    }

    public void importData(List<String> importFile, Boolean overwrite, ZooKeeper zk) throws IOException, InterruptedException, KeeperException {

        for (String line : importFile) {
            logger.debug("Importing line " + line);
            // Delete Operation
            if (line.startsWith("-")) {
                String nodeToDelete = line.substring(1);
                deleteNodeIfExists(nodeToDelete, zk);
            } else {
                int firstEq = line.indexOf('=');
                int secEq = line.indexOf('=', firstEq + 1);

                String path = line.substring(0, firstEq);
                if ("/".equals(path)) {
                    path = "";
                }
                String name = line.substring(firstEq + 1, secEq);
                String value = readExternalizedNodeValue(line.substring(secEq + 1));
                String fullNodePath = path + "/" + name;

                // Skip import of system node
                if (fullNodePath.startsWith(ZK_SYSTEM_NODE)) {
                    logger.debug("Skipping System Node Import: " + fullNodePath);
                    continue;
                }
                boolean nodeExists = nodeExists(fullNodePath, zk);

                if (!nodeExists) {
                    //If node doesnt exist then create it.
                    createPathAndNode(path, name, value.getBytes(), true, zk);
                } else {
                    //If node exists then update only if overwrite flag is set.
                    if (overwrite) {
                        setPropertyValue(path + "/", name, value, zk);
                    } else {
                        logger.info("Skipping update for existing property " + path + "/" + name + " as overwrite is not enabled!");
                    }
                }

            }

        }
    }

    private String readExternalizedNodeValue(String raw) {
        return raw.replaceAll("\\\\n", "\n");
    }

    private void createPathAndNode(String path, String name, byte[] data, boolean force, ZooKeeper zk) throws InterruptedException, KeeperException {
        // 1. Create path nodes if necessary
        StringBuilder currPath = new StringBuilder();
        for (String folder : path.split("/")) {
            if (folder.length() == 0) {
                continue;
            }
            currPath.append('/');
            currPath.append(folder);

            if (!nodeExists(currPath.toString(), zk)) {
                createIfDoesntExist(currPath.toString(), new byte[0], true, zk);
            }
        }

        // 2. Create leaf node
        createIfDoesntExist(path + '/' + name, data, force, zk);
    }

    private void createIfDoesntExist(String path, byte[] data, boolean force, ZooKeeper zooKeeper) throws InterruptedException, KeeperException {
        try {
            zooKeeper.create(path, data, defaultAcl(), CreateMode.PERSISTENT);
        } catch (KeeperException ke) {
            //Explicit Overwrite
            if (KeeperException.Code.NODEEXISTS.equals(ke.code())) {
                if (force) {
                    zooKeeper.delete(path, -1);
                    zooKeeper.create(path, data, defaultAcl(), CreateMode.PERSISTENT);
                }
            } else {
                throw ke;
            }
        }
    }

    public ZKNode listNodeEntries(ZooKeeper zk, String path, String authRole) throws KeeperException, InterruptedException {
        List<String> folders = new ArrayList<>();
        List<LeafBean> leaves = new ArrayList<>();

        List<String> children = zk.getChildren(path, false);
        if (children != null) {
            for (String child : children) {
                if (!child.equals(ZK_SYSTEM_NODE)) {

                    List<String> subChildren = zk.getChildren(path + ("/".equals(path) ? "" : "/") + child, false);
                    boolean isFolder = subChildren != null && !subChildren.isEmpty();
                    if (isFolder) {
                        folders.add(child);
                    } else {
                        String childPath = getNodePath(path, child);
                        leaves.add(this.getNodeValue(zk, path, childPath, child, authRole));
                    }

                }

            }
        }

        Collections.sort(folders);
        Collections.sort(leaves, new Comparator<LeafBean>() {
            @Override
            public int compare(LeafBean o1, LeafBean o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        ZKNode zkNode = new ZKNode();
        zkNode.setLeafBeanLSt(leaves);
        zkNode.setNodeLst(folders);
        return zkNode;
    }

    @Deprecated
    public List<LeafBean> listLeaves(ZooKeeper zk, String path, String authRole) throws InterruptedException, KeeperException {
        List<LeafBean> leaves = new ArrayList<>();

        List<String> children = zk.getChildren(path, false);
        if (children != null) {
            for (String child : children) {
                String childPath = getNodePath(path, child);
                List<String> subChildren = Collections.emptyList();
                subChildren = zk.getChildren(childPath, false);
                boolean isFolder = subChildren != null && !subChildren.isEmpty();
                if (!isFolder) {
                    leaves.add(this.getNodeValue(zk, path, childPath, child, authRole));
                }
            }
        }

        Collections.sort(leaves, new Comparator<LeafBean>() {
            @Override
            public int compare(LeafBean o1, LeafBean o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        return leaves;
    }

    @Deprecated
    public List<String> listFolders(ZooKeeper zk, String path) throws KeeperException, InterruptedException {
        List<String> folders = new ArrayList<>();
        List<String> children = zk.getChildren(path, false);
        if (children != null) {
            for (String child : children) {
                if (!child.equals(ZK_SYSTEM_NODE)) {
                    List<String> subChildren = zk.getChildren(path + ("/".equals(path) ? "" : "/") + child, false);
                    boolean isFolder = subChildren != null && !subChildren.isEmpty();
                    if (isFolder) {
                        folders.add(child);
                    }
                }

            }
        }

        Collections.sort(folders);
        return folders;
    }

    public String getNodePath(String path, String name) {
        return path + ("/".equals(path) ? "" : "/") + name;

    }

    public LeafBean getNodeValue(ZooKeeper zk, String path, String childPath, String child, String authRole) {
        //Reason exception is caught here is so that lookup can continue to happen if a particular property is not found at parent level.
        try {
            logger.trace("Lookup: path=" + path + ",childPath=" + childPath + ",child=" + child + ",authRole=" + authRole);
            byte[] dataBytes = zk.getData(childPath, false, new Stat());
            if (!authRole.equals(ROLE_ADMIN)) {
                if (checkIfPwdField(child)) {
                    return (new LeafBean(path, child, SOPA_PIPA.getBytes()));
                } else {
                    return (new LeafBean(path, child, dataBytes));
                }
            } else {
                return (new LeafBean(path, child, dataBytes));
            }
        } catch (KeeperException | InterruptedException ex) {
            logger.error(ex.getMessage());
        }
        return null;

    }

    public Boolean checkIfPwdField(String property) {
        if (property.contains("PWD") || property.contains("pwd") || property.contains("PASSWORD") || property.contains("password") || property.contains("PASSWD") || property.contains("passwd")) {
            return true;
        } else {
            return false;
        }
    }

    public void createNode(String path, String name, String value, ZooKeeper zk) throws KeeperException, InterruptedException {
        String nodePath = path + name;
        logger.debug("Creating node " + nodePath + " with value " + value);
        zk.create(nodePath, value == null ? null : value.getBytes(), defaultAcl(), CreateMode.PERSISTENT);

    }

    public void createFolder(String folderPath, String propertyName, String propertyValue, ZooKeeper zk) throws KeeperException, InterruptedException {

        logger.debug("Creating folder " + folderPath + " with property " + propertyName + " and value " + propertyValue);
        zk.create(folderPath, "".getBytes(), defaultAcl(), CreateMode.PERSISTENT);
        zk.create(folderPath + "/" + propertyName, propertyValue == null ? null : propertyValue.getBytes(), defaultAcl(), CreateMode.PERSISTENT);

    }

    public void setPropertyValue(String path, String name, String value, ZooKeeper zk) throws KeeperException, InterruptedException {
        String nodePath = path + name;
        logger.debug("Setting property " + nodePath + " to " + value);
        zk.setData(nodePath, value.getBytes(), -1);

    }

    public boolean nodeExists(String nodeFullPath, ZooKeeper zk) throws KeeperException, InterruptedException {
        logger.trace("Checking if exists: " + nodeFullPath);
        return zk.exists(nodeFullPath, false) != null;
    }

    public void deleteFolders(List<String> folderNames, ZooKeeper zk) throws KeeperException, InterruptedException {

        for (String folderPath : folderNames) {
            deleteFolderInternal(folderPath, zk);
        }

    }

    private void deleteFolderInternal(String folderPath, ZooKeeper zk) throws KeeperException, InterruptedException {

        logger.debug("Deleting folder " + folderPath);
        for (String child : zk.getChildren(folderPath, false)) {
            deleteFolderInternal(getNodePath(folderPath, child), zk);
        }
        zk.delete(folderPath, -1);
    }

    public void deleteLeaves(List<String> leafNames, ZooKeeper zk) throws InterruptedException, KeeperException {

        for (String leafPath : leafNames) {
            logger.debug("Deleting leaf " + leafPath);
            zk.delete(leafPath, -1);
        }
    }

    private void deleteNodeIfExists(String path, ZooKeeper zk) throws InterruptedException, KeeperException {
        zk.delete(path, -1);
    }

    public void closeZooKeeper(ZooKeeper zk) throws InterruptedException {
        logger.trace("Closing ZooKeeper");
        if (zk != null) {
            zk.close();
            logger.trace("Closed ZooKeeper");

        }
    }
}
