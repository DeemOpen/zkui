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
package com.deem.zkui.dao;

import com.deem.zkui.domain.History;
import com.googlecode.flyway.core.Flyway;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.javalite.activejdbc.Base;
import org.slf4j.LoggerFactory;

public class Dao {
    
    private final static Integer FETCH_LIMIT = 50;
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(Dao.class);
    private final Properties globalProps;
    
    public Dao(Properties globalProps) {
        this.globalProps = globalProps;
    }
    
    public void open() {
        Base.open(globalProps.getProperty("jdbcClass"), globalProps.getProperty("jdbcUrl"), globalProps.getProperty("jdbcUser"), globalProps.getProperty("jdbcPwd"));
    }
    
    public void close() {
        Base.close();
    }
    
    public void checkNCreate() {
        try {
            Flyway flyway = new Flyway();
            flyway.setDataSource(globalProps.getProperty("jdbcUrl"), globalProps.getProperty("jdbcUser"), globalProps.getProperty("jdbcPwd"));
            //Will wipe db each time. Avoid this in prod.
            if (globalProps.getProperty("env").equals("dev")) {
                flyway.clean();
            }
            //Remove the above line if deploying to prod.
            flyway.migrate();
        } catch (Exception ex) {
            logger.error("Error trying to migrate db! Not severe hence proceeding forward.");
        }
        
    }
    
    public List<History> fetchHistoryRecords() {
        this.open();
        List<History> history = History.findAll().orderBy("ID desc").limit(FETCH_LIMIT);
        history.size();
        this.close();
        return history;
        
    }
    
    public List<History> fetchHistoryRecordsByNode(String historyNode) {
        this.open();
        List<History> history = History.where("CHANGE_SUMMARY like ?", historyNode).orderBy("ID desc").limit(FETCH_LIMIT);
        history.size();
        this.close();
        return history;
    }
    
    public void insertHistory(String user, String ipAddress, String summary) {
        try {
            this.open();
            //To avoid errors due to truncation.
            if (summary.length() >= 500) {
                summary = summary.substring(0, 500);
            }
            History history = new History();
            history.setChangeUser(user);
            history.setChangeIp(ipAddress);
            history.setChangeSummary(summary);
            history.setChangeDate(new Date());
            history.save();
            this.close();
        } catch (Exception ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
        }
        
    }
}
