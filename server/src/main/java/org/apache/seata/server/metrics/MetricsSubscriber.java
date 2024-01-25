/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.common.eventbus.Subscribe;
import org.apache.seata.core.event.ExceptionEvent;
import org.apache.seata.core.event.GlobalTransactionEvent;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.metrics.registry.Registry;
import org.apache.seata.server.event.EventBusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.seata.metrics.IdConstants.APP_ID_KEY;
import static org.apache.seata.metrics.IdConstants.GROUP_KEY;
import static org.apache.seata.metrics.IdConstants.STATUS_VALUE_AFTER_COMMITTED_KEY;
import static org.apache.seata.metrics.IdConstants.STATUS_VALUE_AFTER_ROLLBACKED_KEY;

/**
 * Event subscriber for metrics
 *
 */
public class MetricsSubscriber {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsSubscriber.class);
    private final Registry registry;

    private final Map<String, Consumer<GlobalTransactionEvent>> consumers;

    public MetricsSubscriber(Registry registry) {
        this.registry = registry;
        consumers = new HashMap<>();
        consumers.put(GlobalStatus.Begin.name(), this::processGlobalStatusBegin);
        consumers.put(GlobalStatus.Committed.name(), this::processGlobalStatusCommitted);
        consumers.put(GlobalStatus.Rollbacked.name(), this::processGlobalStatusRollbacked);

        consumers.put(GlobalStatus.CommitFailed.name(), this::processGlobalStatusCommitFailed);
        consumers.put(GlobalStatus.RollbackFailed.name(), this::processGlobalStatusRollbackFailed);
        consumers.put(GlobalStatus.TimeoutRollbacked.name(), this::processGlobalStatusTimeoutRollbacked);
        consumers.put(GlobalStatus.TimeoutRollbackFailed.name(), this::processGlobalStatusTimeoutRollbackFailed);

        consumers.put(GlobalStatus.CommitRetryTimeout.name(), this::processGlobalStatusCommitRetryTimeout);
        consumers.put(GlobalStatus.RollbackRetryTimeout.name(), this::processGlobalStatusTimeoutRollbackRetryTimeout);

        consumers.put(STATUS_VALUE_AFTER_COMMITTED_KEY, this::processAfterGlobalCommitted);
        consumers.put(STATUS_VALUE_AFTER_ROLLBACKED_KEY, this::processAfterGlobalRollbacked);
    }

    private void processGlobalStatusBegin(GlobalTransactionEvent event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("accept new event,xid:{},event:{}", event.getId(), event);
            for (Object object : EventBusManager.get().getSubscribers()) {
                LOGGER.debug("subscribe:{},threadName:{}", object.toString(), Thread.currentThread().getName());
            }
        }
        registry.getCounter(MeterIdConstants.COUNTER_ACTIVE.withTag(APP_ID_KEY, event.getApplicationId())
            .withTag(GROUP_KEY, event.getGroup())).increase(1);
    }

    private void processGlobalStatusCommitted(GlobalTransactionEvent event) {
        if (event.isRetryGlobal()) {
            return;
        }
        decreaseActive(event);
        registry.getCounter(MeterIdConstants.COUNTER_COMMITTED
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
        registry.getSummary(MeterIdConstants.SUMMARY_COMMITTED
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(MeterIdConstants.TIMER_COMMITTED
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processGlobalStatusRollbacked(GlobalTransactionEvent event) {
        if (event.isRetryGlobal()) {
            return;
        }
        decreaseActive(event);
        registry.getCounter(MeterIdConstants.COUNTER_ROLLBACKED
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
        registry.getSummary(MeterIdConstants.SUMMARY_ROLLBACKED
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(MeterIdConstants.TIMER_ROLLBACK
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processAfterGlobalRollbacked(GlobalTransactionEvent event) {
        if (event.isRetryGlobal() && event.isRetryBranch()) {
            decreaseActive(event);
        }
        registry.getCounter(MeterIdConstants.COUNTER_AFTER_ROLLBACKED
            .withTag(APP_ID_KEY, event.getApplicationId())
            .withTag(GROUP_KEY, event.getGroup())).increase(1);
        registry.getSummary(MeterIdConstants.SUMMARY_AFTER_ROLLBACKED
            .withTag(APP_ID_KEY, event.getApplicationId())
            .withTag(GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(MeterIdConstants.TIMER_AFTER_ROLLBACKED
            .withTag(APP_ID_KEY, event.getApplicationId())
            .withTag(GROUP_KEY, event.getGroup()))
            .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processAfterGlobalCommitted(GlobalTransactionEvent event) {
        if (event.isRetryGlobal() && event.isRetryBranch()) {
            decreaseActive(event);
        }
        registry.getCounter(MeterIdConstants.COUNTER_AFTER_COMMITTED
            .withTag(APP_ID_KEY, event.getApplicationId())
            .withTag(GROUP_KEY, event.getGroup())).increase(1);
        registry.getSummary(MeterIdConstants.SUMMARY_AFTER_COMMITTED
            .withTag(APP_ID_KEY, event.getApplicationId())
            .withTag(GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(MeterIdConstants.TIMER_AFTER_COMMITTED
            .withTag(APP_ID_KEY, event.getApplicationId())
            .withTag(GROUP_KEY, event.getGroup()))
            .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processGlobalStatusCommitFailed(GlobalTransactionEvent event) {
        decreaseActive(event);
        reportFailed(event);
    }

    private void processGlobalStatusRollbackFailed(GlobalTransactionEvent event) {
        decreaseActive(event);
        reportFailed(event);
    }

    private void processGlobalStatusTimeoutRollbacked(GlobalTransactionEvent event) {
        decreaseActive(event);
    }

    private void processGlobalStatusTimeoutRollbackFailed(GlobalTransactionEvent event) {
        decreaseActive(event);
        reportTwoPhaseTimeout(event);
    }

    private void processGlobalStatusCommitRetryTimeout(GlobalTransactionEvent event) {
        decreaseActive(event);
        reportTwoPhaseTimeout(event);
    }

    private void processGlobalStatusTimeoutRollbackRetryTimeout(GlobalTransactionEvent event) {
        decreaseActive(event);
    }

    private void decreaseActive(GlobalTransactionEvent event) {
        registry.getCounter(MeterIdConstants.COUNTER_ACTIVE
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).decrease(1);
    }

    private void reportFailed(GlobalTransactionEvent event) {
        registry.getSummary(MeterIdConstants.SUMMARY_FAILED
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(MeterIdConstants.TIMER_FAILED
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void reportTwoPhaseTimeout(GlobalTransactionEvent event) {
        registry.getSummary(MeterIdConstants.SUMMARY_TWO_PHASE_TIMEOUT
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
    }



    @Subscribe
    public void recordGlobalTransactionEventForMetrics(GlobalTransactionEvent event) {
        if (registry != null && consumers.containsKey(event.getStatus())) {
            consumers.get(event.getStatus()).accept(event);
        }
    }


    @Subscribe
    public void exceptionEventForMetrics(ExceptionEvent event) {
        registry.getSummary(MeterIdConstants.SUMMARY_EXP
                .withTag(APP_ID_KEY, event.getName())).increase(1);
    }

    @Override
    public boolean equals(Object obj) {
        return this.getClass().getName().equals(obj.getClass().getName());
    }

    /**
     * PMD check
     * SuppressWarnings("checkstyle:EqualsHashCode")
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
