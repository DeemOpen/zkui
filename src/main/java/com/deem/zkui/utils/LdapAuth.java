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

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.slf4j.LoggerFactory;

public class LdapAuth {

    DirContext ctx = null;
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(LdapAuth.class);

    public boolean authenticateUser(String ldapUrl, String username, String password, String domains) {

        String[] domainArr = domains.split(",");
        for (String domain : domainArr) {
            Hashtable env = new Hashtable();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, ldapUrl);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, domain + "\\" + username);
            env.put(Context.SECURITY_CREDENTIALS, password);
            try {
                ctx = new InitialDirContext(env);
                return true;
            } catch (NamingException e) {

            } finally {
                if (ctx != null) {
                    try {
                        ctx.close();
                    } catch (NamingException ex) {
                        logger.warn(ex.getMessage());
                    }
                }
            }
        }
        return false;

    }
}
