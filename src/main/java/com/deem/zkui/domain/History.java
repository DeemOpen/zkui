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

import java.util.Date;
import org.javalite.activejdbc.Model;

public class History extends Model {

    private Long id;
    private String changeUser;
    private Date changeDate;
    private String changeSummary;
    private String changeIp;

    @Override
    public Long getId() {
        this.id = super.getLong("ID");
        return id;
    }

    public void setId(Long id) {
        super.setLong("ID", id);
    }

    public String getChangeUser() {
        this.changeUser = super.getString("CHANGE_USER");
        return changeUser;
    }

    public void setChangeUser(String changeUser) {
        super.setString("CHANGE_USER", changeUser);
    }

    public Date getChangeDate() {
        this.changeDate = super.getTimestamp("CHANGE_DATE");
        return changeDate;
    }

    public void setChangeDate(Date changeDate) {
        super.setTimestamp("CHANGE_DATE", changeDate);
    }

    public String getChangeSummary() {
        this.changeSummary = super.getString("CHANGE_SUMMARY");
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        super.setString("CHANGE_SUMMARY", changeSummary);
    }

    public String getChangeIp() {
        this.changeIp = super.getString("CHANGE_IP");
        return changeIp;
    }

    public void setChangeIp(String changeIp) {
        super.setString("CHANGE_IP", changeIp);
    }

}
