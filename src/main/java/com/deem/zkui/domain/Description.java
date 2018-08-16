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
package com.deem.zkui.domain;

import org.javalite.activejdbc.Model;

public class Description extends Model {

    private Long id;
    private String path;
    private String description;

    @Override
    public Long getId() {
        this.id = super.getLong("ID");
        return id;
    }

    public void setId(Long id) {
        super.setLong("ID", id);
    }

    public String getPath() {
        this.path = super.getString("PATH");
        return path;
    }

    public void setPath(String path) {
        super.setString("PATH", path);
    }

    public String getDescription() {
        this.description = super.getString("DESCRIPTION");
        return description;
    }

    public void setDescription(String description) {
        super.setString("DESCRIPTION", description);
    }

}
