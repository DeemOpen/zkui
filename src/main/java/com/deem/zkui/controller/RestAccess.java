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
package com.deem.zkui.controller;

import com.deem.zkui.utils.ServletUtil;
import com.deem.zkui.utils.ZooKeeperUtil;
import com.deem.zkui.vo.LeafBean;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = {"/acd/appconfig"})
public class RestAccess extends HttpServlet {

    private final static Logger logger = LoggerFactory.getLogger(RestAccess.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Rest Action!");
        ZooKeeper zk = null;
        try {
            Properties globalProps = (Properties) this.getServletContext().getAttribute("globalProps");
            String zkServer = globalProps.getProperty("zkServer");
            String[] zkServerLst = zkServer.split(",");
            String accessRole = ZooKeeperUtil.ROLE_USER;
            if ((globalProps.getProperty("blockPwdOverRest") != null) && (Boolean.valueOf(globalProps.getProperty("blockPwdOverRest")) == Boolean.FALSE)) {
                accessRole = ZooKeeperUtil.ROLE_ADMIN;
            }
            StringBuilder resultOut = new StringBuilder();
            String clusterName = request.getParameter("cluster");
            String appName = request.getParameter("app");
            String hostName = request.getParameter("host");
            String[] propNames = request.getParameterValues("propNames");
            String propValue = "";
            LeafBean propertyNode;

            if (hostName == null) {
                hostName = ServletUtil.INSTANCE.getRemoteAddr(request);
            }
            zk = ServletUtil.INSTANCE.getZookeeper(request, response, zkServerLst[0], globalProps);
            //get the path of the hosts entry.
            LeafBean hostsNode = null;
            //If app name is mentioned then lookup path is appended with it.
            if (appName != null && ZooKeeperUtil.INSTANCE.nodeExists(ZooKeeperUtil.ZK_HOSTS + "/" + hostName + ":" + appName, zk)) {
                hostsNode = ZooKeeperUtil.INSTANCE.getNodeValue(zk, ZooKeeperUtil.ZK_HOSTS, ZooKeeperUtil.ZK_HOSTS + "/" + hostName + ":" + appName, hostName + ":" + appName, accessRole);
            } else {
                hostsNode = ZooKeeperUtil.INSTANCE.getNodeValue(zk, ZooKeeperUtil.ZK_HOSTS, ZooKeeperUtil.ZK_HOSTS + "/" + hostName, hostName, accessRole);
            }

            String lookupPath = hostsNode.getStrValue();
            logger.trace("Root Path:" + lookupPath);
            String[] pathElements = lookupPath.split("/");

            //Form all combinations of search path you want to look up the property in.
            List<String> searchPath = new ArrayList<>();

            StringBuilder pathSubSet = new StringBuilder();
            for (String pathElement : pathElements) {
                pathSubSet.append(pathElement);
                pathSubSet.append("/");
                searchPath.add(pathSubSet.substring(0, pathSubSet.length() - 1));
            }

            //You specify a cluster or an app name to group.
            if (clusterName != null && appName == null) {
                if (ZooKeeperUtil.INSTANCE.nodeExists(lookupPath + "/" + hostName, zk)) {
                    searchPath.add(lookupPath + "/" + hostName);
                }
                if (ZooKeeperUtil.INSTANCE.nodeExists(lookupPath + "/" + clusterName, zk)) {
                    searchPath.add(lookupPath + "/" + clusterName);
                }
                if (ZooKeeperUtil.INSTANCE.nodeExists(lookupPath + "/" + clusterName + "/" + hostName, zk)) {
                    searchPath.add(lookupPath + "/" + clusterName + "/" + hostName);
                }

            } else if (appName != null && clusterName == null) {

                if (ZooKeeperUtil.INSTANCE.nodeExists(lookupPath + "/" + hostName, zk)) {
                    searchPath.add(lookupPath + "/" + hostName);
                }
                if (ZooKeeperUtil.INSTANCE.nodeExists(lookupPath + "/" + appName, zk)) {
                    searchPath.add(lookupPath + "/" + appName);
                }
                if (ZooKeeperUtil.INSTANCE.nodeExists(lookupPath + "/" + appName + "/" + hostName, zk)) {
                    searchPath.add(lookupPath + "/" + appName + "/" + hostName);
                }

            } else if (appName != null && clusterName != null) {
                //Order in which these paths are listed is important as the lookup happens in that order.
                //Precedence is give to cluster over app.
                if (ZooKeeperUtil.INSTANCE.nodeExists(lookupPath + "/" + hostName, zk)) {
                    searchPath.add(lookupPath + "/" + hostName);
                }
                if (ZooKeeperUtil.INSTANCE.nodeExists(lookupPath + "/" + appName, zk)) {
                    searchPath.add(lookupPath + "/" + appName);
                }
                if (ZooKeeperUtil.INSTANCE.nodeExists(lookupPath + "/" + appName + "/" + hostName, zk)) {
                    searchPath.add(lookupPath + "/" + appName + "/" + hostName);
                }
                if (ZooKeeperUtil.INSTANCE.nodeExists(lookupPath + "/" + clusterName, zk)) {
                    searchPath.add(lookupPath + "/" + clusterName);
                }
                if (ZooKeeperUtil.INSTANCE.nodeExists(lookupPath + "/" + clusterName + "/" + hostName, zk)) {
                    searchPath.add(lookupPath + "/" + clusterName + "/" + hostName);
                }
                if (ZooKeeperUtil.INSTANCE.nodeExists(lookupPath + "/" + clusterName + "/" + appName, zk)) {
                    searchPath.add(lookupPath + "/" + clusterName + "/" + appName);
                }
                if (ZooKeeperUtil.INSTANCE.nodeExists(lookupPath + "/" + clusterName + "/" + appName + "/" + hostName, zk)) {
                    searchPath.add(lookupPath + "/" + clusterName + "/" + appName + "/" + hostName);
                }

            }

            //Search the property in all lookup paths.
            for (String propName : propNames) {
                propValue = null;
                for (String path : searchPath) {
                    logger.trace("Looking up " + path);
                    propertyNode = ZooKeeperUtil.INSTANCE.getNodeValue(zk, path, path + "/" + propName, propName, accessRole);
                    if (propertyNode != null) {
                        propValue = propertyNode.getStrValue();
                    }
                }
                if (propValue != null) {
                    resultOut.append(propName).append("=").append(propValue).append("\n");
                }

            }

            response.setContentType("text/plain;charset=UTF-8");
            try (PrintWriter out = response.getWriter()) {
                out.write(resultOut.toString());
            }

        } catch (KeeperException | InterruptedException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
            ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
        } finally {
            if (zk != null) {
                ServletUtil.INSTANCE.closeZookeeper(zk);
            }
        }

    }
}
