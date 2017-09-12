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
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.Set;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = {"/export"})
public class Export extends HttpServlet {

    private final static Logger logger = LoggerFactory.getLogger(Export.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Export Get Action!");
        try {
            Properties globalProps = (Properties) this.getServletContext().getAttribute("globalProps");
            String zkServer = globalProps.getProperty("zkServer");
            String[] zkServerLst = zkServer.split(",");

            String authRole = (String) request.getSession().getAttribute("authRole");
            if (authRole == null) {
                authRole = ZooKeeperUtil.ROLE_USER;
            }

            String keyDescPath = globalProps.getProperty("keyDesc");
            if (keyDescPath != null && !"".equals(keyDescPath)) {
                keyDescPath = ("/" + keyDescPath + "/");
            }

            String zkPath = request.getParameter("zkPath");
            StringBuilder output = new StringBuilder();
            output.append("#App Config Dashboard (ACD) dump created on :").append(new Date()).append("\n");
            ZooKeeper zk = ServletUtil.INSTANCE.getZookeeper(request, response, zkServerLst[0], globalProps);
            Set<LeafBean> leaves = ZooKeeperUtil.INSTANCE.exportTree(zkPath, zk, authRole);
            for (LeafBean leaf : leaves) {
                output.append(leaf.getPath()).append('=').append(leaf.getName()).append('=').append(ServletUtil.INSTANCE.externalizeNodeValue(leaf.getValue())).append('\n');
                if (keyDescPath != null && !"".equals(keyDescPath)) {
                    LeafBean nodeValue = ZooKeeperUtil.INSTANCE.getNodeValue(zk, keyDescPath, keyDescPath + leaf.getPath() + "/" + leaf.getName(), leaf.getName(), authRole);
                    if (nodeValue != null) {
                        output.append(ZooKeeperUtil.INSTANCE.pathFormat(keyDescPath + leaf.getPath())).append('=').append(nodeValue.getName()).append('=').append(ServletUtil.INSTANCE.externalizeNodeValue(nodeValue.getValue())).append('\n');
                    }
                }
            }// for all leaves
            response.setContentType("text/plain;charset=UTF-8");
            try (PrintWriter out = response.getWriter()) {
                out.write(output.toString());
            }

        } catch (InterruptedException | KeeperException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
            ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
        }
    }
}
