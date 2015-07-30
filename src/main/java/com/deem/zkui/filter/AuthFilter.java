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
package com.deem.zkui.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebFilter(filterName = "filteranno", urlPatterns = "/*")
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig fc) throws ServletException {
        //Do Nothing
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain fc) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (!request.getRequestURI().contains("/login") && !request.getRequestURI().contains("/acd/appconfig")) {
            RequestDispatcher dispatcher;
            HttpSession session = request.getSession();
            if (session != null) {
                if (session.getAttribute("authName") == null || session.getAttribute("authRole") == null) {
                    response.sendRedirect("/login");
                    return;
                }

            } else {
                request.setAttribute("fail_msg", "Session timed out!");
                dispatcher = request.getRequestDispatcher("/Login");
                dispatcher.forward(request, response);
                return;
            }
        }

        fc.doFilter(req, res);
    }

    @Override
    public void destroy() {
        //Do nothing
    }

}
