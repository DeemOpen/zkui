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
package com.deem.zkui.vo;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeafBean implements Comparable<LeafBean> {

    private final static Logger logger = LoggerFactory.getLogger(LeafBean.class);
    private String path;
    private String name;
    private byte[] value;
    private String strValue;

    public LeafBean(String path, String name, byte[] value) {
        super();
        this.path = path;
        this.name = name;
        this.value = value;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public String getStrValue() {
        try {
            return new String(this.value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
        }
        return null;
    }

    public void setStrValue(String strValue) {
        this.strValue = strValue;
    }

    @Override
    public int compareTo(LeafBean o) {
        return (this.path + this.name).compareTo((o.path + o.name));
    }
}
