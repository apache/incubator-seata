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

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
    private Map<String, String> vgroupMapping=new HashMap(){

    };
    /**
     * group list
     */
    private Map<String, String> grouplist;
    /**
     * degrade current not support
     */
    private boolean enableDegrade = false;
    /**
     * disable globalTransaction
     */
    private boolean disableGlobalTransaction = false;

    public Map<String, String> getVgroupMapping() {
        return vgroupMapping;
    }

    public void setVgroupMapping(Map<String, String> vgroupMapping) {
        this.vgroupMapping = vgroupMapping;
    }

    public Map<String, String> getGrouplist() {
        return grouplist;
    }

    public void setGrouplist(Map<String, String> grouplist) {
        this.grouplist = grouplist;
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
}
