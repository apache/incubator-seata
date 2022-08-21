package io.seata.metrics;

public interface TMMeterIdConstants {
    Id COUNTER_ACTIVE = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_ACTIVE);
    Id COUNTER_BEGIN = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_BEGIN);

    Id COUNTER_BEGIN_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_BEGIN_SUCCESS);

    Id COUNTER_BEGIN_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_BEGIN_FAILED);

    Id COUNTER_COMMITTING = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_COMMITTING);

    Id COUNTER_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_COMMITTED);

    Id COUNTER_COMMIT_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_COMMIT_FAILED);

    Id COUNTER_ROLLBACKED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_ROLLBACKED);

    Id COUNTER_AFTER_ROLLBACKED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_AFTER_ROLLBACKED_KEY);

    Id COUNTER_AFTER_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_AFTER_COMMITTED_KEY);

    Id TIMER_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_COMMITTED);

    Id TIMER_ROLLBACK = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_ROLLBACKED);

    Id TIMER_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_FAILED);

    Id TIMER_AFTER_ROLLBACKED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_AFTER_ROLLBACKED_KEY);

    Id TIMER_AFTER_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_AFTER_COMMITTED_KEY);
}
