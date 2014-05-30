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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.zookeeper.ZooKeeper;
import com.deem.zkui.utils.ServletUtil;
import com.deem.zkui.utils.ZooKeeperUtil;
import com.deem.zkui.vo.LeafBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings("serial")
@WebServlet(urlPatterns = {"/acd/appconfig"})
public class RestAccess extends HttpServlet {

    private final static Logger logger = LoggerFactory.getLogger(RestAccess.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Rest Action!");
        try {
            Properties globalProps = (Properties) this.getServletContext().getAttribute("globalProps");
            String zkServer = globalProps.getProperty("zkServer");
            String[] zkServerLst = zkServer.split(",");

            String clusterName = request.getParameter("cluster");
            String appName = request.getParameter("app");
            String hostName = request.getParameter("host");
            String propNames = request.getParameter("propNames");
            String propValue = "";

            if (hostName == null) {
                hostName = ServletUtil.INSTANCE.getRemoteAddr(request);
            }
            ZooKeeper zk = ServletUtil.INSTANCE.getZookeeper(request, response, zkServerLst[0]);
            //get the path of the hosts entry.
            LeafBean hostsNode = ZooKeeperUtil.INSTANCE.getNodeValue(zk, ZooKeeperUtil.ZK_HOSTS, ZooKeeperUtil.ZK_HOSTS + "/" + hostName, hostName, ZooKeeperUtil.ROLE_USER);
            StringBuilder lookupPath = new StringBuilder(hostsNode.getStrValue());
            //You specify a cluster or an app name to group.
            if (clusterName != null) {
                lookupPath.append("/").append(clusterName).append("/").append(hostName);
            }
            if (appName != null) {
                lookupPath.append("/").append(appName).append("/").append(hostName);
            }

            StringBuffer concatPath = new StringBuffer();
            LeafBean propertyNode;
            String[] pathElements = lookupPath.toString().split("/");
            for (String path : pathElements) {
                concatPath.append(path).append("/");
                propertyNode = ZooKeeperUtil.INSTANCE.getNodeValue(zk, concatPath.toString(), concatPath + propNames, propNames, ZooKeeperUtil.ROLE_USER);
                if (propertyNode != null) {
                    propValue = propertyNode.getStrValue();
                }
            }
            response.setContentType("text/plain");
            try (PrintWriter out = response.getWriter()) {
                out.write(propNames + "=" + propValue + "\n");
            }

        } catch (Exception ex) {
            ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
        }

    }
}
