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

    Id TIMER_BEGIN_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_BEGIN_SUCCESS);

    Id TIMER_BEGIN_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_BEGIN_FAILED);


    Id TIMER_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_COMMIT_SUCCESS);

    Id TIMER_COMMIT_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_COMMIT_FAILED);

    Id TIMER_ROLLBACKED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_ROLLBACK_SUCCESS);

    Id TIMER_ROLLBACKFAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_ROLLBACK_FAILED);

    Id TIMER_REPORT_SUCCESS = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_REPORT_SUCCESS);

    Id TIMER_REPORT_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.METRICS_EVENT_STATUS_KEY, IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_REPORT_FAILED);
}
