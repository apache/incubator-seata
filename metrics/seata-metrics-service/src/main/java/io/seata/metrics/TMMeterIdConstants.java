package io.seata.metrics;

public interface TMMeterIdConstants {
    Id COUNTER_ACTIVE = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.STATUS_VALUE_ACTIVE);

    Id COUNTER_BEGIN_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_BEGIN_SUCCESS);

    Id COUNTER_BEGIN_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_BEGIN_FAILED);


    Id COUNTER_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_COMMIT_SUCCESS);

    Id COUNTER_COMMIT_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_COMMIT_FAILED);

    Id COUNTER_ROLLBACKED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_ROLLBACK_SUCCESS);

    Id COUNTER_ROLLBACKFAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_ROLLBACK_FAILED);

    Id COUNTER_REPORT_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_REPORT_SUCCESS);

    Id COUNTER_REPORT_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_REPORT_FAILED);


    Id COUNTER_AFTER_ROLLBACKED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.STATUS_VALUE_AFTER_ROLLBACKED_KEY);

    Id COUNTER_AFTER_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.STATUS_VALUE_AFTER_COMMITTED_KEY);

    Id TIMER_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.STATUS_VALUE_COMMITTED);

    Id TIMER_ROLLBACK = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.STATUS_VALUE_ROLLBACKED);

    Id TIMER_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.STATUS_VALUE_FAILED);

    Id TIMER_AFTER_ROLLBACKED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.STATUS_VALUE_AFTER_ROLLBACKED_KEY);

    Id TIMER_AFTER_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.STATUS_VALUE_AFTER_COMMITTED_KEY);
}
