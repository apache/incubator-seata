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
package io.seata.metrics.service;

import io.seata.common.ConfigurationKeys;
import io.seata.config.ConfigurationFactory;
import io.seata.core.event.BranchEvent;
import io.seata.core.event.EventBus;
import io.seata.core.event.GlobalTransactionEvent;
import io.seata.core.model.BranchType;
import io.seata.metrics.event.EventBusManager;

public class MetricsPublisher {

    private static final EventBus EVENT_BUS = EventBusManager.get();

    public static void postGlobalTransaction(String name, long beginTime, long endTime, String metricEvent, String status) {
        EVENT_BUS.post(new GlobalTransactionEvent(GlobalTransactionEvent.ROLE_TM, name,
                getApplicationId(), getTxServiceGroup(), beginTime,endTime,status, metricEvent,false, false));
    }

    public static void postBranchEvent(String name,BranchType branchType, long beginTime, long endTime,String metricEvent, String status) {
        if (branchType == null) {
            branchType = BranchType.AT;
        }
        EVENT_BUS.post(new BranchEvent(GlobalTransactionEvent.ROLE_TM, name,
                getApplicationId(), getTxServiceGroup(), beginTime,endTime, branchType, status,metricEvent ,false, false));
    }
    private static String getApplicationId() {
        return ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.APPLICATION_ID) == null ? "null":
                ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.APPLICATION_ID);
    }
    private static String getTxServiceGroup() {
        return ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.TX_SERVICE_GROUP) == null ? "null":
                ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.TX_SERVICE_GROUP);
    }
}
