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
import io.seata.metrics.Id;
import io.seata.metrics.registry.Registry;
import io.seata.server.event.EventBusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.metrics.IdConstants.APP_ID_KEY;
import static io.seata.metrics.IdConstants.GROUP_KEY;
import static io.seata.metrics.IdConstants.STATUS_VALUE_AFTER_COMMITTED_KEY;
import static io.seata.metrics.IdConstants.STATUS_VALUE_AFTER_ROLLBACKED_KEY;

/**
 * Event subscriber for metrics
 *
 * @author zhengyangyong
 */
public class MetricsSubscriber {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsSubscriber.class);
    private final Registry registry;

    private final Map<String, Consumer<GlobalTransactionEvent>> consumers;

    public MetricsSubscriber(Registry registry) {
        this.registry = registry;
        this.consumers = initializeConsumers();
    }

    private Map<String, Consumer<GlobalTransactionEvent>> initializeConsumers() {
        Map<String, Consumer<GlobalTransactionEvent>> consumerMap = new HashMap<>();
        consumerMap.put(GlobalStatus.Begin.name(), this::processGlobalStatusBegin);
        consumerMap.put(GlobalStatus.Committed.name(), this::processGlobalStatusCommitted);
        consumerMap.put(GlobalStatus.Rollbacked.name(), this::processGlobalStatusRollbacked);

        consumerMap.put(GlobalStatus.CommitFailed.name(), this::processGlobalStatusCommitFailed);
        consumerMap.put(GlobalStatus.RollbackFailed.name(), this::processGlobalStatusRollbackFailed);
        consumerMap.put(GlobalStatus.TimeoutRollbacked.name(), this::processGlobalStatusTimeoutRollbacked);
        consumerMap.put(GlobalStatus.TimeoutRollbackFailed.name(), this::processGlobalStatusTimeoutRollbackFailed);

        consumerMap.put(GlobalStatus.CommitRetryTimeout.name(), this::processGlobalStatusCommitRetryTimeout);
        consumerMap.put(GlobalStatus.RollbackRetryTimeout.name(), this::processGlobalStatusTimeoutRollbackRetryTimeout);

        consumerMap.put(STATUS_VALUE_AFTER_COMMITTED_KEY, this::processAfterGlobalCommitted);
        consumerMap.put(STATUS_VALUE_AFTER_ROLLBACKED_KEY, this::processAfterGlobalRollbacked);
        return consumerMap;
    }

    private void increaseCounter(Id counterId, GlobalTransactionEvent event) {
        registry.getCounter(
            counterId.withTag(APP_ID_KEY, event.getApplicationId()).withTag(GROUP_KEY, event.getGroup())).increase(1);
    }

    private void decreaseCounter(Id counterId, GlobalTransactionEvent event) {
        registry.getCounter(
            counterId.withTag(APP_ID_KEY, event.getApplicationId()).withTag(GROUP_KEY, event.getGroup())).decrease(1);
    }

    private void increaseSummary(Id summaryId, GlobalTransactionEvent event, long value) {
        registry.getSummary(
            summaryId.withTag(APP_ID_KEY, event.getApplicationId()).withTag(GROUP_KEY, event.getGroup())).increase(
            value);
    }

    private void increaseTimer(Id timerId, GlobalTransactionEvent event) {
        registry.getTimer(timerId.withTag(APP_ID_KEY, event.getApplicationId()).withTag(GROUP_KEY, event.getGroup()))
            .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processGlobalStatusBegin(GlobalTransactionEvent event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("accept new event,xid:{},event:{}", event.getId(), event);
            for (Object object : EventBusManager.get().getSubscribers()) {
                LOGGER.debug("subscribe:{},threadName:{}", object.toString(), Thread.currentThread().getName());
            }
        }
        increaseCounter(MeterIdConstants.COUNTER_ACTIVE, event);
    }

    private void processGlobalStatusCommitted(GlobalTransactionEvent event) {
        if (event.isRetryGlobal()) {
            return;
        }
        decreaseCounter(MeterIdConstants.COUNTER_ACTIVE, event);
        increaseCounter(MeterIdConstants.COUNTER_COMMITTED, event);
        increaseSummary(MeterIdConstants.SUMMARY_COMMITTED, event, 1);
        increaseTimer(MeterIdConstants.TIMER_COMMITTED, event);
    }

    private void processGlobalStatusRollbacked(GlobalTransactionEvent event) {
        if (event.isRetryGlobal()) {
            return;
        }
        decreaseCounter(MeterIdConstants.COUNTER_ACTIVE, event);
        increaseCounter(MeterIdConstants.COUNTER_ROLLBACKED, event);
        increaseSummary(MeterIdConstants.SUMMARY_ROLLBACKED, event, 1);
        increaseTimer(MeterIdConstants.TIMER_ROLLBACKED, event);
    }

    private void processAfterGlobalRollbacked(GlobalTransactionEvent event) {
        if (event.isRetryGlobal() && event.isRetryBranch()) {
            decreaseCounter(MeterIdConstants.COUNTER_ACTIVE, event);
        }
        increaseCounter(MeterIdConstants.COUNTER_AFTER_ROLLBACKED, event);
        increaseSummary(MeterIdConstants.SUMMARY_AFTER_ROLLBACKED, event, 1);
        increaseTimer(MeterIdConstants.TIMER_AFTER_ROLLBACKED, event);
    }

    private void processAfterGlobalCommitted(GlobalTransactionEvent event) {
        if (event.isRetryGlobal() && event.isRetryBranch()) {
            decreaseCounter(MeterIdConstants.COUNTER_ACTIVE, event);
        }
        increaseCounter(MeterIdConstants.COUNTER_AFTER_COMMITTED, event);
        increaseSummary(MeterIdConstants.SUMMARY_AFTER_COMMITTED, event, 1);
        increaseTimer(MeterIdConstants.TIMER_AFTER_COMMITTED, event);
    }

    private void processGlobalStatusCommitFailed(GlobalTransactionEvent event) {
        decreaseCounter(MeterIdConstants.COUNTER_ACTIVE, event);
        reportFailed(event);
    }

    private void processGlobalStatusRollbackFailed(GlobalTransactionEvent event) {
        decreaseCounter(MeterIdConstants.COUNTER_ACTIVE, event);
        reportFailed(event);
    }

    private void processGlobalStatusTimeoutRollbacked(GlobalTransactionEvent event) {
        decreaseCounter(MeterIdConstants.COUNTER_ACTIVE, event);
    }

    private void processGlobalStatusTimeoutRollbackFailed(GlobalTransactionEvent event) {
        decreaseCounter(MeterIdConstants.COUNTER_ACTIVE, event);
        increaseSummary(MeterIdConstants.SUMMARY_TWO_PHASE_TIMEOUT, event, 1);
        reportFailed(event);
    }

    private void processGlobalStatusCommitRetryTimeout(GlobalTransactionEvent event) {
        decreaseCounter(MeterIdConstants.COUNTER_ACTIVE, event);
        increaseSummary(MeterIdConstants.SUMMARY_TWO_PHASE_TIMEOUT, event, 1);
        //The phase 2 retry timeout state should be considered a transaction failed
        reportFailed(event);
    }

    private void processGlobalStatusTimeoutRollbackRetryTimeout(GlobalTransactionEvent event) {
        decreaseCounter(MeterIdConstants.COUNTER_ACTIVE, event);
        increaseSummary(MeterIdConstants.SUMMARY_TWO_PHASE_TIMEOUT, event, 1);
        //The phase 2 retry timeout state should be considered a transaction failed
        reportFailed(event);
    }

    private void reportFailed(GlobalTransactionEvent event) {
        increaseSummary(MeterIdConstants.SUMMARY_FAILED, event, 1);
        increaseTimer(MeterIdConstants.TIMER_FAILED, event);
    }

    @Subscribe
    public void recordGlobalTransactionEventForMetrics(GlobalTransactionEvent event) {
        if (registry != null && consumers.containsKey(event.getStatus())) {
            consumers.get(event.getStatus()).accept(event);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return this.getClass().getName().equals(obj.getClass().getName());
    }

    /**
     * PMD check
     * SuppressWarnings("checkstyle:EqualsHashCode")
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
