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

import java.net.Socket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum CmdUtil {

    INSTANCE;
    private final static Logger logger = LoggerFactory.getLogger(CmdUtil.class);

    public String executeCmd(String cmd, String zkServer, String zkPort) throws IOException {
        StringBuilder sb;
        try (Socket s = new Socket(zkServer, Integer.parseInt(zkPort)); PrintWriter out = new PrintWriter(s.getOutputStream(), true); BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            out.println(cmd);
            String line = reader.readLine();
            sb = new StringBuilder();
            while (line != null) {
                sb.append(line);
                sb.append("<br/>");
                line = reader.readLine();
            }
        }
        return sb.toString();
    }
}
