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
import io.seata.server.event.EventBusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.metrics.IdConstants.APP_ID_KEY;
import static io.seata.metrics.IdConstants.GROUP_KEY;

/**
 * Event subscriber for metrics
 *
 * @author zhengyangyong
 */
public class MetricsSubscriber {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsSubscriber.class);
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

        consumers.put(GlobalStatus.CommitRetryTimeout, this::processGlobalStatusCommitRetryTimeout);
        consumers.put(GlobalStatus.RollbackRetryTimeout, this::processGlobalStatusTimeoutRollbackRetryTimeout);
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

    private void processGlobalStatusCommitFailed(GlobalTransactionEvent event) {
        decreaseActive(event);
        increaseFail(event);
    }

    private void processGlobalStatusRollbackFailed(GlobalTransactionEvent event) {
        decreaseActive(event);
        increaseFail(event);
    }

    private void processGlobalStatusTimeoutRollbacked(GlobalTransactionEvent event) {
        decreaseActive(event);
    }

    private void processGlobalStatusTimeoutRollbackFailed(GlobalTransactionEvent event) {
        decreaseActive(event);
        increaseTimeout(event);
    }

    private void processGlobalStatusCommitRetryTimeout(GlobalTransactionEvent event) {
        decreaseActive(event);
        increaseTimeout(event);
    }

    private void processGlobalStatusTimeoutRollbackRetryTimeout(GlobalTransactionEvent event) {
        decreaseActive(event);
    }

    private void increaseFail(GlobalTransactionEvent event) {
        registry.getCounter(MeterIdConstants.COUNTER_FAILED
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
    }

    private void increaseTimeout(GlobalTransactionEvent event) {
        registry.getCounter(MeterIdConstants.COUNTER_2PHASE_TIMEOUT
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
    }

    private void decreaseActive(GlobalTransactionEvent event) {
        registry.getCounter(MeterIdConstants.COUNTER_ACTIVE
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).decrease(1);
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
     * @return
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
