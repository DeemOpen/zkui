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
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.zookeeper.ZooKeeper;
import com.deem.zkui.utils.ServletUtil;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = {"/logout"})
public class Logout extends HttpServlet {

    private final static Logger logger = LoggerFactory.getLogger(Logout.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            logger.debug("Logout Action!");
            Properties globalProps = (Properties) getServletContext().getAttribute("globalProps");
            String zkServer = globalProps.getProperty("zkServer");
            String[] zkServerLst = zkServer.split(",");
            ZooKeeper zk = ServletUtil.INSTANCE.getZookeeper(request, response, zkServerLst[0],globalProps);
            request.getSession().invalidate();
            zk.close();
            response.sendRedirect("/login");
        } catch (InterruptedException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
            ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
        }

    }
}
