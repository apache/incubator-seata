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
                ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.APPLICATION_ID),
                ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.TX_SERVICE_GROUP),
                beginTime,endTime,status, metricEvent,false, false));
    }

    public static void postBranchEvent(String name,BranchType branchType, long beginTime, long endTime,String metricEvent, String status) {
        EVENT_BUS.post(new BranchEvent(GlobalTransactionEvent.ROLE_TM, name,
                ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.APPLICATION_ID),
                ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.TX_SERVICE_GROUP),
                beginTime,endTime, branchType, status,metricEvent ,false, false));
    }

}
