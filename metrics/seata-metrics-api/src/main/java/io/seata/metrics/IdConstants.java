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
package io.seata.metrics;

/**
 * Seata metrics constants for id
 *
 * @author zhengyangyong
 */
public interface IdConstants {
    String SEATA_TRANSACTION = "seata.transaction";

    String APP_ID_KEY = "applicationId";
    
    String GROUP_KEY = "group";
    String BRANCH_TYPE_KEY = "branchType";

    String NAME_KEY = "name";

    String ROLE_KEY = "role";

    String METER_KEY = "meter";

    String STATISTIC_KEY = "statistic";

    String STATUS_KEY = "status";

    String ROLE_VALUE_TC = "tc";

    String ROLE_VALUE_TM = "tm";

    String ROLE_VALUE_RM = "rm";

    String METER_VALUE_GAUGE = "gauge";

    String METER_VALUE_COUNTER = "counter";

    String METER_VALUE_SUMMARY = "summary";

    String METER_VALUE_TIMER = "timer";

    String STATISTIC_VALUE_COUNT = "count";

    String STATISTIC_VALUE_TOTAL = "total";

    String STATISTIC_VALUE_TPS = "tps";

    String STATISTIC_VALUE_MAX = "max";

    String STATISTIC_VALUE_AVERAGE = "average";

    String STATUS_VALUE_ACTIVE = "active";

    String STATUS_VALUE_COMMITTED = "committed";

    String STATUS_VALUE_ROLLBACKED = "rollbacked";

    String STATUS_VALUE_FAILED = "failed";

    String STATUS_VALUE_TWO_PHASE_TIMEOUT = "2phaseTimeout";

    String RETRY_KEY = "retry";

    String STATUS_VALUE_AFTER_COMMITTED_KEY = "AfterCommitted";

    String STATUS_VALUE_AFTER_ROLLBACKED_KEY = "AfterRollbacked";

    String METRICS_EVENT_STATUS_KEY = "metricsEventStatus";

    String METRICS_EVENT_STATUS_VALUE_GLOBAL_BEGIN_SUCCESS = "globalBeginSuccess";
    String METRICS_EVENT_STATUS_VALUE_GLOBAL_BEGIN_FAILED = "globalBeginFailed";
    String METRICS_EVENT_STATUS_VALUE_GLOBAL_COMMIT_SUCCESS= "globalCommitSuccess";
    String METRICS_EVENT_STATUS_VALUE_GLOBAL_COMMIT_FAILED = "globalCommitFailed";
    String METRICS_EVENT_STATUS_VALUE_GLOBAL_ROLLBACK_SUCCESS= "globalRollbackSuccess";
    String METRICS_EVENT_STATUS_VALUE_GLOBAL_ROLLBACK_FAILED = "globalRollbackFailed";
    String METRICS_EVENT_STATUS_VALUE_GLOBAL_REPORT_SUCCESS= "globalReportSuccess";
    String METRICS_EVENT_STATUS_VALUE_GLOBAL_REPORT_FAILED = "globalReportFailed";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_REGISTER_SUCCESS = "branchRegisterSuccess";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_REGISTER_FAILED = "branchRegisterFailed";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_REPORT_SUCCESS = "branchReportSuccess";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_REPORT_FAILED = "branchReportFailed";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_DELETE_SUCCESS = "undoLogDeleteSuccess";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_DELETE_FAILED = "undoLogDeleteFailed";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_BATCH_DELETE_SUCCESS = "undoLogBatchDeleteSuccess";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_BATCH_DELETE_FAILED = "undoLogBatchDeleteFailed";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_INSERT_SUCCESS = "undoLogInsertSuccess";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_INSERT_FAILED = "undoLogInsertFailed";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_EXECUTE_SUCCESS = "undoLogExecuteSuccess";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_EXECUTE_FAILED = "undoLogExecuteFailed";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_SUCCESS = "branchRollbackSuccess";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_FAILED = "branchRollbackFailed";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_SUCCESS = "branchCommitSuccess";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_FAILED = "branchCommitFailed";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_INSERT_TCC_FENCE_SUCCESS = "insertTCCFenceSuccess";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_INSERT_TCC_FENCE_FAILED = "insertTCCFenceFailed";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_TCC_FENCE_SUCCESS_ON_ALREADY_COMMITTED = "commitTCCFenceSuccessOnAlreadyCommitted";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_TCC_FENCE_SUCCESS = "commitTCCFenceSuccess";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_TCC_FENCE_FAILED = "commitTCCFenceFailed";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_TCC_FENCE_FAILED_ON_ALREADY_ROLLBACK = "commitTCCFenceFailedOnRollback";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_FAILED_ON_ALREADY_COMMITTED = "rollbackTCCFenceFailedOnCommitted";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_FAILED_ON_INSERT = "rollbackTCCFenceFailedOnInsert";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_FAILED_ON_UPDATE = "rollbackTCCFenceFailedOnUpdate";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_SUCCESS_ON_ALREADY_ROLLBACK = "rollbackTCCFenceSuccessOnRollback";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_SUCCESS_ON_INSERT = "rollbackTCCFenceSuccessOnInsert";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_SUCCESS_ON_UPDATE = "rollbackTCCFenceSuccessOnUpdate";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_DELETE_TCC_FENCE_SUCCESS = "deleteTCCFenceSuccess";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_DELETE_TCC_FENCE_FAILED = "deleteTCCFenceFailed";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_DELETE_TCC_FENCE_BY_DATE_SUCCESS = "deleteTCCFenceByDateSuccess";
    String METRICS_EVENT_STATUS_VALUE_BRANCH_DELETE_TCC_FENCE_BY_DATE_FAILED = "deleteTCCFenceByDateFailed";
}
