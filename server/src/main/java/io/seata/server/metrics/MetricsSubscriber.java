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
package io.seata.server.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.common.eventbus.Subscribe;
import io.seata.core.event.GlobalTransactionEvent;
import io.seata.core.model.GlobalStatus;
import io.seata.metrics.registry.Registry;

/**
 * Event subscriber for metrics
 *
 * @author zhengyangyong
 */
public class MetricsSubscriber {
    private final Registry registry;

    private final Map<GlobalStatus, Consumer<GlobalTransactionEvent>> consumers;

    public MetricsSubscriber(Registry registry) {
        this.registry = registry;
        consumers = new HashMap<>();
        consumers.put(GlobalStatus.Begin, this::processGlobalStatusBegin);
        consumers.put(GlobalStatus.Committed, this::processGlobalStatusCommitted);
        consumers.put(GlobalStatus.Rollbacked, this::processGlobalStatusRollbacked);

        consumers.put(GlobalStatus.CommitFailed, this::processGlobalStatusCommitFailed);
        consumers.put(GlobalStatus.RollbackFailed, this::processGlobalStatusRollbackFailed);
        consumers.put(GlobalStatus.TimeoutRollbacked, this::processGlobalStatusTimeoutRollbacked);
        consumers.put(GlobalStatus.TimeoutRollbackFailed, this::processGlobalStatusTimeoutRollbackFailed);
    }

    private void processGlobalStatusBegin(GlobalTransactionEvent event) {
        registry.getCounter(MeterIdConstants.COUNTER_ACTIVE).increase(1);
    }

    private void processGlobalStatusCommitted(GlobalTransactionEvent event) {
        registry.getCounter(MeterIdConstants.COUNTER_ACTIVE).decrease(1);
        registry.getCounter(MeterIdConstants.COUNTER_COMMITTED).increase(1);
        registry.getSummary(MeterIdConstants.SUMMARY_COMMITTED).increase(1);
        registry.getTimer(MeterIdConstants.TIMER_COMMITTED).record(event.getEndTime() - event.getBeginTime(),
            TimeUnit.MILLISECONDS);
    }

    private void processGlobalStatusRollbacked(GlobalTransactionEvent event) {
        registry.getCounter(MeterIdConstants.COUNTER_ACTIVE).decrease(1);
        registry.getCounter(MeterIdConstants.COUNTER_ROLLBACKED).increase(1);
        registry.getSummary(MeterIdConstants.SUMMARY_ROLLBACKED).increase(1);
        registry.getTimer(MeterIdConstants.TIMER_ROLLBACK).record(event.getEndTime() - event.getBeginTime(),
            TimeUnit.MILLISECONDS);
    }

    private void processGlobalStatusCommitFailed(GlobalTransactionEvent event) {
        registry.getCounter(MeterIdConstants.COUNTER_ACTIVE).decrease(1);
    }

    private void processGlobalStatusRollbackFailed(GlobalTransactionEvent event) {
        registry.getCounter(MeterIdConstants.COUNTER_ACTIVE).decrease(1);
    }

    private void processGlobalStatusTimeoutRollbacked(GlobalTransactionEvent event) {
        registry.getCounter(MeterIdConstants.COUNTER_ACTIVE).decrease(1);
    }

    private void processGlobalStatusTimeoutRollbackFailed(GlobalTransactionEvent event) {
        registry.getCounter(MeterIdConstants.COUNTER_ACTIVE).decrease(1);
    }

    @Subscribe
    public void recordGlobalTransactionEventForMetrics(GlobalTransactionEvent event) {
        if (registry != null && consumers.containsKey(event.getStatus())) {
            consumers.get(event.getStatus()).accept(event);
        }
    }
}
