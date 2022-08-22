package io.seata.metrics;

public interface RMMeterIdConstants {
    Id COUNTER_ACTIVE = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_ACTIVE);

    Id COUNTER_REGISTER_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_BRANCH_REGISTER_SUCCESS);

    Id COUNTER_REGISTER_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_BRANCH_REGISTER_FAILED);

    Id COUNTER_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_COMMITTED);

    Id COUNTER_ROLLBACKED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_ROLLBACKED);

}
