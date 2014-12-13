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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;

import org.slf4j.LoggerFactory;

public class LdapAuth {

    DirContext ctx = null;
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(LdapAuth.class);

    public List<String> authenticateUserAndReturnGroups(String ldapUrl, String username, String password, String domains,
        String principalTemplate, String groupsToTest, String groupMatchAttrId, String groupSearchBase) {

        List<String> matchedGroups = new ArrayList<String>();
        String[] groupsToTestArr = new String[]{};
        if (groupsToTest != null && groupsToTest.trim().length() > 0) {
          groupsToTestArr = groupsToTest.trim().split(",");
        }
        String[] domainArr = domains.trim().split(",");
        boolean successfulLogin = false;
        for (String domain : domainArr) {
            Hashtable env = new Hashtable();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, ldapUrl);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, String.format(principalTemplate, domain, username));
            env.put(Context.SECURITY_CREDENTIALS, password);
            try {
                ctx = new InitialDirContext(env);
                // We only need one successful login
                successfulLogin = true;
                groupLoop: for (String group : groupsToTestArr) {
                    Attributes matchAttrs = new BasicAttributes(true);
                    matchAttrs.put(groupMatchAttrId, group);

                    Enumeration<SearchResult> results = ctx.search(groupSearchBase, matchAttrs);
                    while (results.hasMoreElements()) {
                        SearchResult result = results.nextElement();
                        Attribute attr = result.getAttributes().get("memberUid");
                        for (int i = 0; i < attr.size(); i++) {
                            if (username.equals(attr.get(i))) {
                                matchedGroups.add(group);
                                continue groupLoop;
                            }
                        }
                    }
                }
            } catch (NamingException e) {
              logger.error("Unable to login over LDAP", e);
            } finally {
                if (ctx != null) {
                    try {
                        ctx.close();
                    } catch (NamingException ex) {
                        logger.warn(ex.getMessage(), ex);
                    }
                }
            }
        }
        if (successfulLogin) {
          return matchedGroups;
        } else {
          return null;
        }
    }
}
