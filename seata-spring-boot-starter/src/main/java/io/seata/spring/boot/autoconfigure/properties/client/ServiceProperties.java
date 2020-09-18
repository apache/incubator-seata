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
package io.seata.spring.boot.autoconfigure.properties.client;

import java.util.HashMap;
import java.util.Map;

import io.seata.common.util.StringUtils;
import io.seata.spring.boot.autoconfigure.properties.SeataProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.common.DefaultValues.DEFAULT_DISABLE_GLOBAL_TRANSACTION;
import static io.seata.common.DefaultValues.DEFAULT_GROUPLIST;
import static io.seata.common.DefaultValues.DEFAULT_TC_CLUSTER;
import static io.seata.common.DefaultValues.DEFAULT_TX_GROUP;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SERVICE_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = SERVICE_PREFIX)
public class ServiceProperties implements InitializingBean {
    /**
     * vgroup->rgroup
     */
    private Map<String, String> vgroupMapping = new HashMap<>();
    /**
     * group list
     */
    private Map<String, String> grouplist = new HashMap<>();
    /**
     * degrade current not support
     */
    private boolean enableDegrade = false;
    /**
     * disable globalTransaction
     */
    private boolean disableGlobalTransaction = DEFAULT_DISABLE_GLOBAL_TRANSACTION;

    @Autowired(required = false)
    private SeataProperties seataProperties;


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

    public void setSeataProperties(SeataProperties seataProperties) {
        this.seataProperties = seataProperties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //Create the default cluster and grouplist for the txServiceGroup.
        if (seataProperties != null && StringUtils.isNotBlank(seataProperties.getTxServiceGroup())) {
            //The transaction service group
            String txServiceGroup = seataProperties.getTxServiceGroup();

            //When cluster is blank, create the default cluster.
            String clusterValue = vgroupMapping.get(txServiceGroup);
            if (StringUtils.isBlank(clusterValue)) {
                clusterValue = DEFAULT_TC_CLUSTER;
                vgroupMapping.put(txServiceGroup, clusterValue);
            }

            //When grouplist is blank, create the default grouplist.
            String grouplistValue = this.grouplist.get(clusterValue);
            if (StringUtils.isBlank(grouplistValue)) {
                grouplistValue = DEFAULT_GROUPLIST;
                grouplist.put(clusterValue, grouplistValue);
            }
        }

        if (vgroupMapping.isEmpty()) {
            vgroupMapping.put(DEFAULT_TX_GROUP, DEFAULT_TC_CLUSTER);
        }
        if (grouplist.isEmpty()) {
            grouplist.put(DEFAULT_TC_CLUSTER, DEFAULT_GROUPLIST);
        }
    }
}
