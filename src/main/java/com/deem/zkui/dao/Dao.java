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

import com.deem.zkui.domain.Description;
import com.deem.zkui.domain.History;
import com.deem.zkui.utils.ZooKeeperUtil;
import com.deem.zkui.vo.LeafBean;
import com.googlecode.flyway.core.Flyway;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.mysql.jdbc.StringUtils;
import org.javalite.activejdbc.Base;
import org.slf4j.LoggerFactory;

public class Dao {
    
    private final static Integer FETCH_LIMIT = 50;
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(Dao.class);
    private final Properties globalProps;
    private int historyLimit = FETCH_LIMIT;
    
    public Dao(Properties globalProps) {
        this.globalProps = globalProps;
        String historyLimit = this.globalProps == null ? null : this.globalProps.getProperty("historyLimit");
        if (StringUtils.isEmptyOrWhitespaceOnly(historyLimit)) {
            try {
                this.historyLimit = Integer.valueOf(historyLimit);
            } catch (Exception e) {}
        }
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
        try {
            this.open();
            List<History> history = History.findAll().orderBy("ID desc").limit(historyLimit);
            history.size();
            return history;
        } catch (Exception ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
        } finally {
            this.close();
        }
        return null;
    }
    
    public List<History> fetchHistoryRecordsByNode(String historyNode) {
        try {
            this.open();
            List<History> history = History.where("CHANGE_SUMMARY like ?", historyNode).orderBy("ID desc").limit(historyLimit);
            history.size();
            return history;
        } catch (Exception ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
        } finally {
            this.close();
        }
        return null;
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
        } catch (Exception ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
        } finally {
            this.close();
        }
    }

    public void putDescription(List<LeafBean> beans) {
        if (beans == null || beans.size() < 1)  return;
        try {
            StringBuffer buffer = new StringBuffer();
            for (LeafBean leaf : beans) {
                buffer.append(((buffer.length() == 0 ? "" : "', '")) + ZooKeeperUtil.INSTANCE.pathFormat(leaf.getPath() + "/" + leaf.getName()));
            }
            this.open();
            List<Description> descriptions = Description.where("PATH IN ('" + buffer.toString() + "')");
            descriptions.size();
            Iterator<Description> iterator = descriptions.iterator();
            while (iterator.hasNext()) {
                Description next = iterator.next();
                for (LeafBean leaf : beans) {
                    if (next.getPath().equals(ZooKeeperUtil.INSTANCE.pathFormat(leaf.getPath() + "/" + leaf.getName()))) {
                        leaf.setDescription(next.getDescription());
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
        } finally {
            this.close();
        }
    }

    public void deleteDescription(String... paths) {
        try {
            StringBuffer buffer = new StringBuffer();
            for (String path : paths) {
                buffer.append((buffer.length() == 0 ? "" : " OR ") + "PATH = '" + ZooKeeperUtil.INSTANCE.pathFormat(path) + "'");
            }
            this.open();
            Description.delete(buffer.toString());
        } catch (Exception ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
        } finally {
            this.close();
        }
    }

    public void deleteDescriptionByNode(String... nodes) {
        try {
            StringBuffer buffer = new StringBuffer();
            for (String node : nodes) {
                buffer.append((buffer.length() == 0 ? "" : " OR ") + "PATH LIKE '" + ZooKeeperUtil.INSTANCE.pathFormat(node) + "%'");
            }
            this.open();
            Description.delete(buffer.toString());
        } catch (Exception ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
        } finally {
            this.close();
        }
    }

    public void insertDescription(String path, String description) {
        if (StringUtils.isEmptyOrWhitespaceOnly(path) || path.length() > 500) {
            return;
        }
        try {
            this.open();
            if (description.length() >= 500) {
                description = description.substring(0, 500);
            }
            Description.delete("PATH = ?", ZooKeeperUtil.INSTANCE.pathFormat(path));
            Description desc = new Description();
            desc.setPath(ZooKeeperUtil.INSTANCE.pathFormat(path));
            desc.setDescription(description);
            desc.save();
        } catch (Exception ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
        } finally {
            this.close();
        }
    }
}
