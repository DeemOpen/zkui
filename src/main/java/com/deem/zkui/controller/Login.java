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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.deem.zkui.utils.ServletUtil;
import com.deem.zkui.utils.ZooKeeperUtil;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.deem.zkui.utils.LdapAuth;

import java.util.Arrays;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = {"/login"})
public class Login extends HttpServlet {

    private final static Logger logger = LoggerFactory.getLogger(Login.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Login Action!");
        try {
            Properties globalProps = (Properties) getServletContext().getAttribute("globalProps");
            Map<String, Object> templateParam = new HashMap<>();
            templateParam.put("uptime", globalProps.getProperty("uptime"));
            templateParam.put("loginMessage", globalProps.getProperty("loginMessage"));
            ServletUtil.INSTANCE.renderHtml(request, response, templateParam, "login.ftl.html");
        } catch (TemplateException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
            ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
        }

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Login Post Action!");
        try {
            Properties globalProps = (Properties) getServletContext().getAttribute("globalProps");
            Map<String, Object> templateParam = new HashMap<>();
            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(Integer.valueOf(globalProps.getProperty("sessionTimeout")));
            //TODO: Implement custom authentication logic if required.
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            String role = null;
            List<String> authenticatedGroups = null;
            boolean authenticated = false;
            //if ldap is provided then it overrides roleset.
            if (globalProps.getProperty("ldapAuth").equals("true")) {
                authenticatedGroups = new LdapAuth().authenticateUserAndReturnGroups(globalProps.getProperty("ldapUrl"), username, password,
                  globalProps.getProperty("ldapDomain"), globalProps.getProperty("ldapPrincipalTemplate"),
                  globalProps.getProperty("ldapGroupsToTest"), globalProps.getProperty("ldapGroupMatchAttrId"),
                  globalProps.getProperty("ldapGroupSearchBase"));
                authenticated = (authenticatedGroups != null);
                logger.info("Authentication for user " + username + " in groups " + authenticatedGroups);
                if (authenticated) {
                    JSONArray jsonRoleSet = (JSONArray) ((JSONObject) new JSONParser().parse(globalProps.getProperty("ldapRoleSet"))).get("users");
                    for (Iterator it = jsonRoleSet.iterator(); it.hasNext();) {
                        JSONObject jsonUser = (JSONObject) it.next();
                        if (jsonUser.get("username") != null && jsonUser.get("username").equals("*")) {
                            role = (String) jsonUser.get("role");
                        }
                        if (jsonUser.get("username") != null && jsonUser.get("username").equals(username)) {
                            role = (String) jsonUser.get("role");
                        }
                    }
                    if (globalProps.getProperty("ldapGroupsToTest") != null && globalProps.getProperty("ldapGroupsToTest").trim().length() > 0) {
                        // If group membership is required, then the user must be a member of one of the groups
                        // This allows a configuration in which not every LDAP user is allowed to access zookeeper
                        if (authenticatedGroups.size() == 0) {
                            logger.info("User mathed no groups, but groups were expected. Rejecting login");
                            authenticated = false;
                        } else {
                            JSONArray jsonGroupRoleSet =
                                    (JSONArray) ((JSONObject) new JSONParser().parse(globalProps.getProperty("ldapGroupRoleSet"))).get("groups");
                            groupRoleLoop: for (Iterator it = jsonGroupRoleSet.iterator(); it.hasNext();) {
                                JSONObject jsonGroupRole = (JSONObject) it.next();
                                if (jsonGroupRole.get("gruopname") != null) {
                                    for (String group : authenticatedGroups) {
                                        if (jsonGroupRole.get("gruopname").equals(group)) {
                                            role = (String) jsonGroupRole.get("role");
                                            // First one wins
                                            break groupRoleLoop;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (role == null) {
                        role = ZooKeeperUtil.ROLE_USER;
                    }
                }
            } else {
                JSONArray jsonRoleSet = (JSONArray) ((JSONObject) new JSONParser().parse(globalProps.getProperty("userSet"))).get("users");
                for (Iterator it = jsonRoleSet.iterator(); it.hasNext();) {
                    JSONObject jsonUser = (JSONObject) it.next();
                    if (jsonUser.get("username").equals(username) && jsonUser.get("password").equals(password)) {
                        authenticated = true;
                        role = (String) jsonUser.get("role");
                    }
                }
            }
            if (authenticated) {
                logger.info("Login successfull: " + username);
                session.setAttribute("authName", username);
                session.setAttribute("authRole", role);
                response.sendRedirect("/home");
            } else {
                session.setAttribute("flashMsg", "Invalid Login");
                ServletUtil.INSTANCE.renderHtml(request, response, templateParam, "login.ftl.html");
            }

        } catch (ParseException | TemplateException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
            ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
        }
    }
}
