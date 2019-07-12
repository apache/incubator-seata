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

import io.seata.metrics.IdConstants;
import io.seata.metrics.Id;

/**
 * Constants for meter id in tc
 *
 * @author zhengyangyong
 */
public class MeterIdConstants {
    public static final Id COUNTER_ACTIVE = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_ACTIVE);

    public static final Id COUNTER_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_COMMITTED);

    public static final Id COUNTER_ROLLBACKED = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_ROLLBACKED);

    public static final Id SUMMARY_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_SUMMARY)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_COMMITTED);

    public static final Id SUMMARY_ROLLBACKED = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_SUMMARY)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_ROLLBACKED);

    public static final Id TIMER_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_COMMITTED);

    public static final Id TIMER_ROLLBACK = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_ROLLBACKED);
}
