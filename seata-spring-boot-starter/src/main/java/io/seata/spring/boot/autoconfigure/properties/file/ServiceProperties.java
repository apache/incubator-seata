/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.spring.boot.autoconfigure.properties.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static io.seata.spring.boot.autoconfigure.StarterConstants.SERVICE_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = SERVICE_PREFIX)
public class ServiceProperties {
    /**
     * vgroup->rgroup
     */
    private String vgroupMapping = "default";
    /**
     * only support single node
     */
    private String grouplist = "127.0.0.1:8091";
    /**
     * degrade current not support
     */
    private boolean enableDegrade = false;
    /**
     * disable globalTransaction
     */
    private boolean disableGlobalTransaction = false;

    private Map<String,Config> groups = new HashMap<>();

    public String getVgroupMapping() {
        return vgroupMapping;
    }

    public ServiceProperties setVgroupMapping(String vgroupMapping) {
        this.vgroupMapping = vgroupMapping;
        return this;
    }

    public String getGrouplist() {
        return grouplist;
    }

    public ServiceProperties setGrouplist(String grouplist) {
        this.grouplist = grouplist;
        return this;
    }

    public boolean isEnableDegrade() {
        return enableDegrade;
    }

    public ServiceProperties setEnableDegrade(boolean enableDegrade) {
        this.enableDegrade = enableDegrade;
        return this;
    }

    public boolean isDisableGlobalTransaction() {
        return disableGlobalTransaction;
    }

    public ServiceProperties setDisableGlobalTransaction(boolean disableGlobalTransaction) {
        this.disableGlobalTransaction = disableGlobalTransaction;
        return this;
    }

    public Map<String, Config> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, Config> groups) {
        this.groups = groups;
    }

    public static class Config {
        private String grouplist;

        public String getGrouplist() {
            return grouplist;
        }

        public void setGrouplist(String grouplist) {
            this.grouplist = grouplist;
        }
    }
}
