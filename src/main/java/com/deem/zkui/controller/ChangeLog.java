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

import freemarker.template.TemplateException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.deem.zkui.dao.Dao;
import com.deem.zkui.domain.History;
import com.deem.zkui.utils.ServletUtil;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = {"/history"})
public class ChangeLog extends HttpServlet {

    private final static Logger logger = LoggerFactory.getLogger(ChangeLog.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("History Get Action!");
        try {
            Properties globalProps = (Properties) this.getServletContext().getAttribute("globalProps");
            Dao dao = new Dao(globalProps);
            Map<String, Object> templateParam = new HashMap<>();
            List<History> historyLst = dao.fetchHistoryRecords();
            templateParam.put("historyLst", historyLst);
            templateParam.put("historyNode", "");
            ServletUtil.INSTANCE.renderHtml(request, response, templateParam, "history.ftl.html");
        } catch (TemplateException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
            ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
        }

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("History Post Action!");
        try {
            Properties globalProps = (Properties) this.getServletContext().getAttribute("globalProps");
            Dao dao = new Dao(globalProps);
            Map<String, Object> templateParam = new HashMap<>();
            String action = request.getParameter("action");
            List<History> historyLst;
            if (action.equals("showhistory")) {

                String historyNode = request.getParameter("historyNode");
                historyLst = dao.fetchHistoryRecordsByNode("%" + historyNode + "%");
                templateParam.put("historyLst", historyLst);
                templateParam.put("historyNode", historyNode);
                ServletUtil.INSTANCE.renderHtml(request, response, templateParam, "history.ftl.html");

            } else {
                response.sendRedirect("/history");
            }
        } catch (TemplateException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
            ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
        }
    }
}
