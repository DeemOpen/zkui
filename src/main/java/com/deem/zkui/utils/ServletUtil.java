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

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum ServletUtil {

    INSTANCE;
    private final static Logger logger = LoggerFactory.getLogger(ServletUtil.class);

    public void renderHtml(HttpServletRequest request, HttpServletResponse response, Map<String, Object> templateParam, String view) throws IOException, TemplateException {

        if (request != null && response != null && templateParam != null) {
            //There is no way to access session info in freemarker template. 
            //Hence all view rendering happens via this function which adds session info to attribute for each request.
            HttpSession session = request.getSession();
            if (session != null) {
                //Flash messages are always set at session level and need to be propgrated to attributes.
                //They are reset after being displayed once.
                if (session.getAttribute("flashMsg") != null) {
                    templateParam.put("flashMsg", session.getAttribute("flashMsg"));
                    session.setAttribute("flashMsg", null);
                }
                templateParam.put("authName", session.getAttribute("authName"));
                templateParam.put("authRole", session.getAttribute("authRole"));

                response.setContentType("text/html;charset=UTF-8");
                
                Template template = null;
                long startTime = System.currentTimeMillis();
                Configuration config = new Configuration();
                config.setClassForTemplateLoading(request.getServletContext().getClass(), "/");
                template = config.getTemplate("/webapp/template/" + view);
                template.process(templateParam, response.getWriter());
                long estimatedTime = System.currentTimeMillis() - startTime;
                logger.trace("Elapsed Time in Secs for Rendering: " + estimatedTime / 1000);

            }
        }

    }

    public void renderError(HttpServletRequest request, HttpServletResponse response, String error) {
        try {
            logger.error("Error :" + error);
            Map<String, Object> templateParam = new HashMap<>();
            response.setContentType("text/html;charset=UTF-8");
            Template template = null;
            Configuration config = new Configuration();
            config.setClassForTemplateLoading(request.getServletContext().getClass(), "/");
            template = config.getTemplate("/webapp/template/error.ftl.html");
            templateParam.put("error", error);
            template.process(templateParam, response.getWriter());
        } catch (TemplateException | IOException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
        }

    }

    public ZooKeeper getZookeeper(HttpServletRequest request, HttpServletResponse response, String zkServer, Properties globalProps) {
        try {

            HttpSession session = request.getSession();
            ZooKeeper zk = (ZooKeeper) session.getAttribute("zk");
            if (zk == null || zk.getState() != ZooKeeper.States.CONNECTED) {
                Integer zkSessionTimeout = Integer.parseInt(globalProps.getProperty("zkSessionTimeout"));
                //Converting seconds to ms.
                zkSessionTimeout = zkSessionTimeout * 1000;
                zk = ZooKeeperUtil.INSTANCE.createZKConnection(zkServer, zkSessionTimeout);
                ZooKeeperUtil.INSTANCE.setDefaultAcl(globalProps.getProperty("defaultAcl"));
                if (zk.getState() != ZooKeeper.States.CONNECTED) {
                    session.setAttribute("zk", null);
                } else {
                    session.setAttribute("zk", zk);
                }

            }
            return zk;
        } catch (IOException | InterruptedException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
        }
        return null;
    }

    public void closeZookeeper(ZooKeeper zk) {
        try {
            zk.close();
        } catch (Exception ex) {
            logger.error("Error in closing zk,will cause problem in zk! " + ex.getMessage());
        }

    }

    public String externalizeNodeValue(byte[] value) {
        return value == null ? "" : new String(value).replaceAll("\\n", "\\\\n").replaceAll("\\r", "");
        // We might want to BASE64 encode it
    }

    //Using X-Forwarded-For to capture IP addresses coming via load balancer.
    public String getRemoteAddr(HttpServletRequest request) {
        String remoteAddr = request.getHeader("X-Forwarded-For");
        if (remoteAddr == null) {
            remoteAddr = request.getRemoteAddr();
        }
        return remoteAddr;
    }

}
