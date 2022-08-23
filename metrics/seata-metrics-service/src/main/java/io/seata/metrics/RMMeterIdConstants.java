package io.seata.metrics;

public interface RMMeterIdConstants {
    Id COUNTER_ACTIVE = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.STATUS_VALUE_ACTIVE);

    Id COUNTER_REGISTER_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_REGISTER_SUCCESS);

    Id COUNTER_REGISTER_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_REGISTER_FAILED);

    Id COUNTER_REPORT_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_REPORT_SUCCESS);

    Id COUNTER_REPORT_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_REPORT_FAILED);

    Id COUNTER_COMMIT_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_SUCCESS);

    Id COUNTER_COMMIT_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_FAILED);

    Id COUNTER_ROLLBACK_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_SUCCESS);

    Id COUNTER_ROLLBACK_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_FAILED);

    Id COUNTER_UNDO_LOG_BATCH_DELETE_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_BATCH_DELETE_SUCCESS);

    Id COUNTER_UNDO_LOG_BATCH_DELETE_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_BATCH_DELETE_FAILED);

    Id COUNTER_UNDO_LOG_DELETE_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_DELETE_SUCCESS);

    Id COUNTER_UNDO_LOG_DELETE_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_DELETE_FAILED);

    Id COUNTER_UNDO_LOG_INSERT_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_INSERT_SUCCESS);

    Id COUNTER_UNDO_LOG_INSERT_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_INSERT_FAILED);

    Id COUNTER_UNDO_LOG_EXECUTE_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_EXECUTE_SUCCESS);

    Id COUNTER_UNDO_LOG_EXECUTE_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_EXECUTE_FAILED);

    Id COUNTER_TCC_INSERT_FENCE_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_INSERT_TCC_FENCE_SUCCESS);

    Id COUNTER_TCC_INSERT_FENCE_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_INSERT_TCC_FENCE_FAILED);

    Id COUNTER_TCC_COMMIT_FENCE_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_SUCCESS);

    Id COUNTER_TCC_COMMIT_FENCE_SUCCESS_ON_ALREADY_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_TCC_FENCE_SUCCESS_ON_ALREADY_COMMITTED);
    Id COUNTER_TCC_COMMIT_FENCE_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_FAILED);

    Id COUNTER_TCC_COMMIT_FENCE_FAILED_ON_ALREADY_ROLLBACK = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_TCC_FENCE_FAILED_ON_ALREADY_ROLLBACK);

    Id COUNTER_TCC_ROLLBACK_FENCE_FAILED_ON_ALREADY_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_FAILED_ON_ALREADY_COMMITTED);

    Id COUNTER_TCC_ROLLBACK_FENCE_FAILED_ON_INSERT = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_FAILED_ON_INSERT);

    Id COUNTER_TCC_ROLLBACK_FENCE_FAILED_ON_UPDATE = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_FAILED_ON_UPDATE);

    Id COUNTER_TCC_ROLLBACK_FENCE_SUCCESS_ON_ALREADY_ROLLBACK = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_FAILED_ON_ALREADY_COMMITTED);

    Id COUNTER_TCC_ROLLBACK_FENCE_SUCCESS_ON_INSERT = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_SUCCESS_ON_INSERT);

    Id COUNTER_TCC_ROLLBACK_FENCE_SUCCESS_ON_UPDATE = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_SUCCESS_ON_UPDATE);

    Id COUNTER_DELETE_TCC_FENCE_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_DELETE_TCC_FENCE_SUCCESS);

    Id COUNTER_DELETE_TCC_FENCE_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_DELETE_TCC_FENCE_FAILED);

    Id COUNTER_DELETE_TCC_FENCE_BY_DATE_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_DELETE_TCC_FENCE_BY_DATE_SUCCESS);

    Id COUNTER_DELETE_TCC_FENCE_BY_DATE_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_DELETE_TCC_FENCE_BY_DATE_FAILED);


    Id TIMER_REGISTER_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_REGISTER_SUCCESS);

    Id TIMER_REGISTER_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_REGISTER_FAILED);

    Id TIMER_REPORT_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_REPORT_SUCCESS);

    Id TIMER_REPORT_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_REPORT_FAILED);

    Id TIMER_COMMIT_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_SUCCESS);

    Id TIMER_COMMIT_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_FAILED);

    Id TIMER_ROLLBACK_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_SUCCESS);

    Id TIMER_ROLLBACK_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_FAILED);

    Id TIMER_UNDO_LOG_BATCH_DELETE_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_BATCH_DELETE_SUCCESS);

    Id TIMER_UNDO_LOG_BATCH_DELETE_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_BATCH_DELETE_FAILED);

    Id TIMER_UNDO_LOG_DELETE_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_DELETE_SUCCESS);

    Id TIMER_UNDO_LOG_DELETE_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_DELETE_FAILED);

    Id TIMER_UNDO_LOG_INSERT_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_INSERT_SUCCESS);

    Id TIMER_UNDO_LOG_INSERT_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_INSERT_FAILED);

    Id TIMER_UNDO_LOG_EXECUTE_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_EXECUTE_SUCCESS);

    Id TIMER_UNDO_LOG_EXECUTE_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_UNDO_LOG_EXECUTE_FAILED);

    Id TIMER_TCC_INSERT_FENCE_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_INSERT_TCC_FENCE_SUCCESS);

    Id TIMER_TCC_INSERT_FENCE_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_INSERT_TCC_FENCE_FAILED);

    Id TIMER_TCC_COMMIT_FENCE_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_SUCCESS);

    Id TIMER_TCC_COMMIT_FENCE_SUCCESS_ON_ALREADY_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_TCC_FENCE_SUCCESS_ON_ALREADY_COMMITTED);
    Id TIMER_TCC_COMMIT_FENCE_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_FAILED);

    Id TIMER_TCC_COMMIT_FENCE_FAILED_ON_ALREADY_ROLLBACK = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_COMMIT_TCC_FENCE_FAILED_ON_ALREADY_ROLLBACK);

    Id TIMER_TCC_ROLLBACK_FENCE_FAILED_ON_ALREADY_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_FAILED_ON_ALREADY_COMMITTED);

    Id TIMER_TCC_ROLLBACK_FENCE_FAILED_ON_INSERT = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_FAILED_ON_INSERT);

    Id TIMER_TCC_ROLLBACK_FENCE_FAILED_ON_UPDATE = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_FAILED_ON_UPDATE);

    Id TIMER_TCC_ROLLBACK_FENCE_SUCCESS_ON_ALREADY_ROLLBACK = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_FAILED_ON_ALREADY_COMMITTED);

    Id TIMER_TCC_ROLLBACK_FENCE_SUCCESS_ON_INSERT = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_SUCCESS_ON_INSERT);

    Id TIMER_TCC_ROLLBACK_FENCE_SUCCESS_ON_UPDATE = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_ROLLBACK_TCC_FENCE_SUCCESS_ON_UPDATE);

    Id TIMER_DELETE_TCC_FENCE_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_DELETE_TCC_FENCE_SUCCESS);

    Id TIMER_DELETE_TCC_FENCE_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_DELETE_TCC_FENCE_FAILED);

    Id TIMER_DELETE_TCC_FENCE_BY_DATE_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_DELETE_TCC_FENCE_BY_DATE_SUCCESS);

    Id TIMER_DELETE_TCC_FENCE_BY_DATE_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_DELETE_TCC_FENCE_BY_DATE_FAILED);
}
