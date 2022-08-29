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
import io.seata.metrics.IdConstants;

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
        if (MetricsManager.get().getRole().equals(MetricsManager.ROLE_VALUE_SERVER)) {
            consumers.put(GlobalStatus.Begin.name(), this::processGlobalStatusBegin);
            consumers.put(GlobalStatus.Committed.name(), this::processGlobalStatusCommitted);
            consumers.put(GlobalStatus.Rollbacked.name(), this::processGlobalStatusRollbacked);

            consumers.put(GlobalStatus.CommitFailed.name(), this::processGlobalStatusCommitFailed);
            consumers.put(GlobalStatus.RollbackFailed.name(), this::processGlobalStatusRollbackFailed);
            consumers.put(GlobalStatus.TimeoutRollbacked.name(), this::processGlobalStatusTimeoutRollbacked);
            consumers.put(GlobalStatus.TimeoutRollbackFailed.name(), this::processGlobalStatusTimeoutRollbackFailed);

            consumers.put(GlobalStatus.CommitRetryTimeout.name(), this::processGlobalStatusCommitRetryTimeout);
            consumers.put(GlobalStatus.RollbackRetryTimeout.name(), this::processGlobalStatusTimeoutRollbackRetryTimeout);

            consumers.put(IdConstants.STATUS_VALUE_AFTER_COMMITTED_KEY, this::processAfterGlobalCommitted);
            consumers.put(IdConstants.STATUS_VALUE_AFTER_ROLLBACKED_KEY, this::processAfterGlobalRollbacked);
        } else if (MetricsManager.get().getRole().equals(MetricsManager.ROLE_VALUE_CLIENT)) {
            consumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_BEGIN_FAILED, this::processClientGlobalStatusBeginFailed);
            consumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_BEGIN_SUCCESS, this::processClientGlobalStatusBeginSuccess);

            consumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_COMMIT_FAILED, this::processClientGlobalStatusCommitFailed);
            consumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_COMMIT_SUCCESS, this::processClientGlobalStatusCommitted);

            consumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_ROLLBACK_FAILED, this::processClientGlobalStatusRollbackFailed);
            consumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_ROLLBACK_SUCCESS, this::processClientGlobalStatusRollbacked);

            consumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_REPORT_FAILED, this::processClientGlobalStatusReportFailed);
            consumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_REPORT_SUCCESS, this::processClientGlobalStatusReportSuccess);

            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_REGISTER_FAILED, this::processClientBranchStatusRegisterFailed);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_REGISTER_SUCCESS, this::processClientBranchStatusRegistered);

            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_REPORT_FAILED, this::processClientBranchStatusReportFailed);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_REPORT_SUCCESS, this::processClientBranchStatusReportSuccess);

            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_BATCH_DELETE_FAILED, this::processClientBranchStatusUndologBatchDeleteFailed);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_BATCH_DELETE_SUCCESS, this::processClientBranchStatusUndologBatchDeleteSuccess);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_DELETE_FAILED, this::processClientBranchStatusUndologDeleteFailed);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_DELETE_SUCCESS, this::processClientBranchStatusUndologDeleteSuccess);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_INSERT_FAILED, this::processClientBranchStatusUndologInsertFailed);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_INSERT_SUCCESS, this::processClientBranchStatusUndologInsertSuccess);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_EXECUTE_FAILED, this::processClientBranchStatusUndologExecuteFailed);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_EXECUTE_SUCCESS, this::processClientBranchStatusUndologExecuteSuccess);

            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_FAILED, this::processClientBranchStatusRollbackFailed);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_SUCCESS, this::processClientBranchStatusRollbackSuccess);

            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_FAILED, this::processClientBranchStatusCommitFailed);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_SUCCESS, this::processClientBranchStatusCommitSuccess);

            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_INSERT_TCC_FENCE_FAILED, this::processClientBranchStatusInsertTCCFenceFailed);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_INSERT_TCC_FENCE_SUCCESS, this::processClientBranchStatusInsertTCCFenceSuccess);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_TCC_FENCE_FAILED, this::processClientBranchStatusCommitTCCFenceFailed);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_TCC_FENCE_FAILED_ON_ALREADY_ROLLBACK, this::processClientBranchStatusCommitTCCFenceFailedOnRollback);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_TCC_FENCE_SUCCESS, this::processClientBranchStatusCommitTCCFenceSuccess);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_TCC_FENCE_SUCCESS_ON_ALREADY_COMMITTED, this::processClientBranchStatusCommitTCCFenceSuccessOnCommitted);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_FAILED_ON_ALREADY_COMMITTED, this::processClientBranchStatusTCCRollbackFenceFailedOnCommitted);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_FAILED_ON_INSERT, this::processClientBranchStatusTCCRollbackFenceFailedOnInsert);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_FAILED_ON_UPDATE, this::processClientBranchStatusTCCRollbackFenceFailedOnUpdate);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_SUCCESS_ON_ALREADY_ROLLBACK, this::processClientBranchStatusTCCRollbackFenceSuccessOnRollback);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_SUCCESS_ON_INSERT, this::processClientBranchStatusTCCRollbackFenceSuccessOnInsert);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_SUCCESS_ON_UPDATE, this::processClientBranchStatusTCCRollbackFenceSuccessOnUpdate);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_DELETE_TCC_FENCE_FAILED, this::processClientBranchStatusTCCDeleteFenceFailed);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_DELETE_TCC_FENCE_SUCCESS, this::processClientBranchStatusTCCDeleteFenceSuccess);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_DELETE_TCC_FENCE_BY_DATE_FAILED, this::processClientBranchStatusTCCDeleteFenceByDateFailed);
            branchConsumers.put(IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_DELETE_TCC_FENCE_BY_DATE_SUCCESS, this::processClientBranchStatusTCCDeleteFenceByDateSuccess);
        }
    }

    private void processClientBranchStatusTCCDeleteFenceByDateSuccess(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_DELETE_TCC_FENCE_BY_DATE_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_DELETE_TCC_FENCE_BY_DATE_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusTCCDeleteFenceByDateFailed(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_DELETE_TCC_FENCE_BY_DATE_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_DELETE_TCC_FENCE_BY_DATE_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusTCCDeleteFenceFailed(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_DELETE_TCC_FENCE_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_DELETE_TCC_FENCE_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusTCCDeleteFenceSuccess(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_DELETE_TCC_FENCE_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_DELETE_TCC_FENCE_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusTCCRollbackFenceSuccessOnUpdate(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_TCC_ROLLBACK_FENCE_SUCCESS_ON_UPDATE.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_TCC_ROLLBACK_FENCE_SUCCESS_ON_UPDATE.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusTCCRollbackFenceSuccessOnInsert(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_TCC_ROLLBACK_FENCE_SUCCESS_ON_INSERT.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_TCC_ROLLBACK_FENCE_SUCCESS_ON_INSERT.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusTCCRollbackFenceSuccessOnRollback(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_TCC_ROLLBACK_FENCE_SUCCESS_ON_ALREADY_ROLLBACK.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_TCC_ROLLBACK_FENCE_SUCCESS_ON_ALREADY_ROLLBACK.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusTCCRollbackFenceFailedOnUpdate(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_TCC_ROLLBACK_FENCE_FAILED_ON_UPDATE.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_TCC_ROLLBACK_FENCE_FAILED_ON_UPDATE.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusTCCRollbackFenceFailedOnInsert(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_TCC_ROLLBACK_FENCE_FAILED_ON_INSERT.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_TCC_ROLLBACK_FENCE_FAILED_ON_INSERT.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusTCCRollbackFenceFailedOnCommitted(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_TCC_ROLLBACK_FENCE_FAILED_ON_ALREADY_COMMITTED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_TCC_ROLLBACK_FENCE_FAILED_ON_ALREADY_COMMITTED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusCommitTCCFenceSuccessOnCommitted(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_TCC_COMMIT_FENCE_SUCCESS_ON_ALREADY_COMMITTED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_TCC_COMMIT_FENCE_SUCCESS_ON_ALREADY_COMMITTED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusCommitTCCFenceSuccess(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_TCC_COMMIT_FENCE_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_TCC_COMMIT_FENCE_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusCommitTCCFenceFailedOnRollback(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_TCC_COMMIT_FENCE_FAILED_ON_ALREADY_ROLLBACK.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_TCC_COMMIT_FENCE_FAILED_ON_ALREADY_ROLLBACK.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusCommitTCCFenceFailed(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_TCC_COMMIT_FENCE_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_TCC_COMMIT_FENCE_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusInsertTCCFenceSuccess(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_TCC_INSERT_FENCE_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_TCC_INSERT_FENCE_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusInsertTCCFenceFailed(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_TCC_INSERT_FENCE_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_TCC_INSERT_FENCE_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusCommitSuccess(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_COMMIT_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_COMMIT_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
        clientDecreaseBranchActive(event);
    }

    private void processClientBranchStatusCommitFailed(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_COMMIT_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_COMMIT_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
        clientDecreaseBranchActive(event);
    }

    private void processClientBranchStatusRollbackFailed(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_ROLLBACK_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_ROLLBACK_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
        clientDecreaseBranchActive(event);
    }

    private void processClientBranchStatusRollbackSuccess(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_ROLLBACK_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_ROLLBACK_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
        clientDecreaseBranchActive(event);
    }

    private void processClientBranchStatusUndologExecuteSuccess(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_UNDO_LOG_EXECUTE_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_UNDO_LOG_EXECUTE_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusUndologExecuteFailed(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_UNDO_LOG_EXECUTE_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_UNDO_LOG_EXECUTE_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusUndologInsertSuccess(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_UNDO_LOG_INSERT_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_UNDO_LOG_INSERT_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusUndologInsertFailed(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_UNDO_LOG_INSERT_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_UNDO_LOG_INSERT_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusUndologBatchDeleteSuccess(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_UNDO_LOG_BATCH_DELETE_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_UNDO_LOG_BATCH_DELETE_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusUndologBatchDeleteFailed(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_UNDO_LOG_BATCH_DELETE_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_UNDO_LOG_BATCH_DELETE_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusUndologDeleteSuccess(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_UNDO_LOG_DELETE_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_UNDO_LOG_DELETE_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusUndologDeleteFailed(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_UNDO_LOG_DELETE_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_UNDO_LOG_DELETE_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusReportFailed(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_REPORT_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_REPORT_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusReportSuccess(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_REPORT_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_REPORT_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusRegistered(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_ACTIVE.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getCounter(RMMeterIdConstants.COUNTER_REGISTER_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_REGISTER_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientBranchStatusRegisterFailed(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_REGISTER_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(RMMeterIdConstants.TIMER_REGISTER_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.BRANCH_TYPE_KEY, event.getBranchType().name())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void clientDecreaseBranchActive(BranchEvent event) {
        registry.getCounter(RMMeterIdConstants.COUNTER_ACTIVE.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).decrease(1);
    }

    private void processClientGlobalStatusReportFailed(GlobalTransactionEvent event) {
        registry.getCounter(TMMeterIdConstants.COUNTER_REPORT_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(TMMeterIdConstants.TIMER_REPORT_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientGlobalStatusReportSuccess(GlobalTransactionEvent event) {
        registry.getCounter(TMMeterIdConstants.COUNTER_REPORT_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(TMMeterIdConstants.TIMER_REPORT_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientGlobalStatusRollbackFailed(GlobalTransactionEvent event) {
        registry.getCounter(TMMeterIdConstants.COUNTER_ROLLBACKFAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(TMMeterIdConstants.TIMER_ROLLBACKFAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
        clientDecreaseGlobalActive(event);
    }

    private void processClientGlobalStatusRollbacked(GlobalTransactionEvent event) {
        registry.getCounter(TMMeterIdConstants.COUNTER_ROLLBACKED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(TMMeterIdConstants.TIMER_ROLLBACKED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
        clientDecreaseGlobalActive(event);
    }


    private void processClientGlobalStatusCommitFailed(GlobalTransactionEvent event) {
        registry.getCounter(TMMeterIdConstants.COUNTER_COMMIT_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(TMMeterIdConstants.TIMER_COMMIT_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
        clientDecreaseGlobalActive(event);
    }

    private void processClientGlobalStatusCommitted(GlobalTransactionEvent event) {
        registry.getCounter(TMMeterIdConstants.COUNTER_COMMITTED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(TMMeterIdConstants.TIMER_COMMITTED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
        clientDecreaseGlobalActive(event);
    }


    private void processClientGlobalStatusBeginFailed(GlobalTransactionEvent event) {
        registry.getCounter(TMMeterIdConstants.COUNTER_BEGIN_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(TMMeterIdConstants.TIMER_BEGIN_FAILED.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processClientGlobalStatusBeginSuccess(GlobalTransactionEvent event) {
        registry.getCounter(TMMeterIdConstants.COUNTER_ACTIVE.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getCounter(TMMeterIdConstants.COUNTER_BEGIN_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(TMMeterIdConstants.TIMER_BEGIN_SUCCESS.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.STATUS_KEY, event.getStatus())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }


    private void clientDecreaseGlobalActive(GlobalTransactionEvent event) {
        registry.getCounter(TMMeterIdConstants.COUNTER_ACTIVE.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).decrease(1);
    }

    private void processGlobalStatusBegin(GlobalTransactionEvent event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("accept new event,xid:{},event:{}", event.getId(), event);
            for (Object object : EventBusManager.get().getSubscribers()) {
                LOGGER.debug("subscribe:{},threadName:{}", object.toString(), Thread.currentThread().getName());
            }
        }
        registry.getCounter(TCMeterIdConstants.COUNTER_ACTIVE.withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
            .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
    }

    private void processGlobalStatusCommitted(GlobalTransactionEvent event) {
        if (event.isRetryGlobal()) {
            return;
        }
        decreaseActive(event);
        registry.getCounter(TCMeterIdConstants.COUNTER_COMMITTED
                .withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getSummary(TCMeterIdConstants.SUMMARY_COMMITTED
                .withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(TCMeterIdConstants.TIMER_COMMITTED
                .withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processGlobalStatusRollbacked(GlobalTransactionEvent event) {
        if (event.isRetryGlobal()) {
            return;
        }
        decreaseActive(event);
        registry.getCounter(TCMeterIdConstants.COUNTER_ROLLBACKED
                .withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getSummary(TCMeterIdConstants.SUMMARY_ROLLBACKED
                .withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(TCMeterIdConstants.TIMER_ROLLBACK
                .withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processAfterGlobalRollbacked(GlobalTransactionEvent event) {
        if (event.isRetryGlobal() && event.isRetryBranch()) {
            decreaseActive(event);
        }
        registry.getCounter(TCMeterIdConstants.COUNTER_AFTER_ROLLBACKED
            .withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
            .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getSummary(TCMeterIdConstants.SUMMARY_AFTER_ROLLBACKED
            .withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
            .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(TCMeterIdConstants.TIMER_AFTER_ROLLBACKED
            .withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
            .withTag(IdConstants.GROUP_KEY, event.getGroup()))
            .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processAfterGlobalCommitted(GlobalTransactionEvent event) {
        if (event.isRetryGlobal() && event.isRetryBranch()) {
            decreaseActive(event);
        }
        registry.getCounter(TCMeterIdConstants.COUNTER_AFTER_COMMITTED
            .withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
            .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getSummary(TCMeterIdConstants.SUMMARY_AFTER_COMMITTED
            .withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
            .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(TCMeterIdConstants.TIMER_AFTER_COMMITTED
            .withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
            .withTag(IdConstants.GROUP_KEY, event.getGroup()))
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
                .withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).decrease(1);
    }

    private void reportFailed(GlobalTransactionEvent event) {
        registry.getSummary(TCMeterIdConstants.SUMMARY_FAILED
                .withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
        registry.getTimer(TCMeterIdConstants.TIMER_FAILED
                .withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.GROUP_KEY, event.getGroup()))
                .record(event.getEndTime() - event.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void reportTwoPhaseTimeout(GlobalTransactionEvent event) {
        registry.getSummary(TCMeterIdConstants.SUMMARY_TWO_PHASE_TIMEOUT
                .withTag(IdConstants.APP_ID_KEY, event.getApplicationId())
                .withTag(IdConstants.GROUP_KEY, event.getGroup())).increase(1);
    }


    @Subscribe
    public void recordGlobalTransactionEventForMetrics(GlobalTransactionEvent event) {
        if (registry != null) {
            if (consumers.containsKey(event.getMetricEvent())) {
                consumers.get(event.getMetricEvent()).accept(event);
            } else if (consumers.containsKey(event.getStatus())) {
                consumers.get(event.getStatus()).accept(event);
            }
        }
    }

    @Subscribe
    public void recordBranchEventForMetrics(BranchEvent event) {
        if (registry != null && branchConsumers.containsKey(event.getMetricEvent())) {
            branchConsumers.get(event.getMetricEvent()).accept(event);
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
