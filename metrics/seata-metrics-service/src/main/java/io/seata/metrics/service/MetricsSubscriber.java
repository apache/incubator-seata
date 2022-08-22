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

import com.google.common.eventbus.Subscribe;
import io.seata.core.event.BranchEvent;
import io.seata.core.event.GlobalTransactionEvent;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.metrics.RMMeterIdConstants;
import io.seata.metrics.TCMeterIdConstants;
import io.seata.metrics.TMMeterIdConstants;
import io.seata.metrics.event.EventBusManager;
import io.seata.metrics.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static io.seata.metrics.IdConstants.APP_ID_KEY;
import static io.seata.metrics.IdConstants.GROUP_KEY;
import static io.seata.metrics.IdConstants.BRANCH_TYPE_KEY;
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
    private final Map<String, Consumer<BranchEvent>> branchConsumers;
    public MetricsSubscriber(Registry registry) {
        this.registry = registry;
        consumers = new HashMap<>();
        branchConsumers = new HashMap<>();
        if (MetricsManager.get().getRole().equals(MetricsManager.get().ROLE_VALUE_SERVER)) {
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
        }else if (MetricsManager.get().getRole().equals(MetricsManager.get().ROLE_VALUE_CLIENT)) {
            consumers.put(GlobalStatus.BeginFailed.name(), this::processClientGlobalStatusBeginFailed);
            consumers.put(GlobalStatus.BeginSuccess.name(), this::processClientGlobalStatusBeginSuccess);

            consumers.put(GlobalStatus.CommitFailed.name(), this::processClientGlobalStatusCommitFailed);
            consumers.put(GlobalStatus.Committed.name(), this::processClientGlobalStatusCommitted);

            consumers.put(GlobalStatus.Rollbacked.name(), this::processClientGlobalStatusRollbacked);
            consumers.put(GlobalStatus.RollbackFailed.name(), this::processClientGlobalStatusRollbackFailed);

            branchConsumers.put(BranchStatus.Registered.name(), this::processClientBranchStatusRegistered);
            branchConsumers.put(BranchStatus.RegisterFailed.name(), this::processClientBranchStatusRegisterFailed);
        }
    }

    private void processClientBranchStatusRegistered(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_ACTIVE.withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
        registry.getCounter(RMMeterIdConstants.COUNTER_REGISTER_SUCCESS.withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
    }

    private void processClientBranchStatusRegisterFailed(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_REGISTER_FAILED.withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
    }

    private void processClientGlobalStatusReport(GlobalTransactionEvent event) {
        registry.getCounter(TMMeterIdConstants.COUNTER_REPORT.withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
    }

    private void processClientGlobalStatusRollbackFailed(GlobalTransactionEvent event) {
        registry.getCounter(TMMeterIdConstants.COUNTER_ROLLBACKFAILED.withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
    }

    private void processClientGlobalStatusRollbacked(GlobalTransactionEvent event) {
        registry.getCounter(TMMeterIdConstants.COUNTER_ROLLBACKED.withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
    }


    private void processClientGlobalStatusCommitFailed(GlobalTransactionEvent event) {
        registry.getCounter(TMMeterIdConstants.COUNTER_COMMIT_FAILED.withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
    }

    private void processClientGlobalStatusCommitted(GlobalTransactionEvent event) {
        registry.getCounter(TMMeterIdConstants.COUNTER_COMMITTED.withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
    }


    private void processClientGlobalStatusBeginFailed(GlobalTransactionEvent event) {
        registry.getCounter(TMMeterIdConstants.COUNTER_BEGIN_FAILED.withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
    }

    private void processClientGlobalStatusBeginSuccess(GlobalTransactionEvent event) {
        registry.getCounter(TMMeterIdConstants.COUNTER_ACTIVE.withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
        registry.getCounter(TMMeterIdConstants.COUNTER_BEGIN_SUCCESS.withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
    }


    private void processGlobalStatusBegin(GlobalTransactionEvent event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("accept new event,xid:{},event:{}", event.getId(), event);
            for (Object object : EventBusManager.get().getSubscribers()) {
                LOGGER.debug("subscribe:{},threadName:{}", object.toString(), Thread.currentThread().getName());
            }
        }
        registry.getCounter(TCMeterIdConstants.COUNTER_ACTIVE.withTag(APP_ID_KEY, event.getApplicationId())
            .withTag(GROUP_KEY, event.getGroup())).increase(1);
    }

    private void processGlobalStatusCommitted(GlobalTransactionEvent event) {
        if (event.isRetryGlobal()) {
            return;
        }
        decreaseActive(event);
        registry.getCounter(TCMeterIdConstants.COUNTER_COMMITTED
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
        registry.getSummary(TCMeterIdConstants.SUMMARY_COMMITTED
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(TCMeterIdConstants.TIMER_COMMITTED
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processGlobalStatusRollbacked(GlobalTransactionEvent event) {
        if (event.isRetryGlobal()) {
            return;
        }
        decreaseActive(event);
        registry.getCounter(TCMeterIdConstants.COUNTER_ROLLBACKED
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
        registry.getSummary(TCMeterIdConstants.SUMMARY_ROLLBACKED
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(TCMeterIdConstants.TIMER_ROLLBACK
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processAfterGlobalRollbacked(GlobalTransactionEvent event) {
        if (event.isRetryGlobal() && event.isRetryBranch()) {
            decreaseActive(event);
        }
        registry.getCounter(TCMeterIdConstants.COUNTER_AFTER_ROLLBACKED
            .withTag(APP_ID_KEY, event.getApplicationId())
            .withTag(GROUP_KEY, event.getGroup())).increase(1);
        registry.getSummary(TCMeterIdConstants.SUMMARY_AFTER_ROLLBACKED
            .withTag(APP_ID_KEY, event.getApplicationId())
            .withTag(GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(TCMeterIdConstants.TIMER_AFTER_ROLLBACKED
            .withTag(APP_ID_KEY, event.getApplicationId())
            .withTag(GROUP_KEY, event.getGroup()))
            .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processAfterGlobalCommitted(GlobalTransactionEvent event) {
        if (event.isRetryGlobal() && event.isRetryBranch()) {
            decreaseActive(event);
        }
        registry.getCounter(TCMeterIdConstants.COUNTER_AFTER_COMMITTED
            .withTag(APP_ID_KEY, event.getApplicationId())
            .withTag(GROUP_KEY, event.getGroup())).increase(1);
        registry.getSummary(TCMeterIdConstants.SUMMARY_AFTER_COMMITTED
            .withTag(APP_ID_KEY, event.getApplicationId())
            .withTag(GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(TCMeterIdConstants.TIMER_AFTER_COMMITTED
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
        registry.getCounter(TCMeterIdConstants.COUNTER_ACTIVE
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).decrease(1);
    }

    private void reportFailed(GlobalTransactionEvent event) {
        registry.getSummary(TCMeterIdConstants.SUMMARY_FAILED
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(TCMeterIdConstants.TIMER_FAILED
                .withTag(APP_ID_KEY, event.getApplicationId())
                .withTag(GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void reportTwoPhaseTimeout(GlobalTransactionEvent event) {
        registry.getSummary(TCMeterIdConstants.SUMMARY_TWO_PHASE_TIMEOUT
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
    public void recordBranchEventForMetrics(BranchEvent event) {
        if (registry != null && branchConsumers.containsKey(event.getStatus())) {
            branchConsumers.get(event.getStatus()).accept(event);
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
