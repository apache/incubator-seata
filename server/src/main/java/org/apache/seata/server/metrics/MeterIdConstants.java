/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server.metrics;

import org.apache.seata.metrics.IdConstants;
import org.apache.seata.metrics.Id;

/**
 * Constants for meter id in tc
 *
 */
public interface MeterIdConstants {
    Id COUNTER_ACTIVE = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_ACTIVE);

    Id COUNTER_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_COMMITTED);

    Id COUNTER_ROLLBACKED = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_ROLLBACKED);

    Id COUNTER_AFTER_ROLLBACKED = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_AFTER_ROLLBACKED_KEY);

    Id COUNTER_AFTER_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_AFTER_COMMITTED_KEY);


    Id SUMMARY_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_SUMMARY)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_COMMITTED);

    Id SUMMARY_ROLLBACKED = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_SUMMARY)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_ROLLBACKED);

    Id SUMMARY_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_SUMMARY)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_FAILED);

    Id SUMMARY_TWO_PHASE_TIMEOUT = new Id(IdConstants.SEATA_TRANSACTION)
         .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
         .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_SUMMARY)
         .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_TWO_PHASE_TIMEOUT);

    Id SUMMARY_AFTER_ROLLBACKED = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_SUMMARY)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_AFTER_ROLLBACKED_KEY);

    Id SUMMARY_AFTER_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_SUMMARY)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_AFTER_COMMITTED_KEY);

    Id TIMER_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_COMMITTED);

    Id TIMER_ROLLBACK = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_ROLLBACKED);

    Id TIMER_FAILED = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_FAILED);

    Id TIMER_AFTER_ROLLBACKED = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_AFTER_ROLLBACKED_KEY);

    Id TIMER_AFTER_COMMITTED = new Id(IdConstants.SEATA_TRANSACTION)
        .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
        .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
        .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_AFTER_COMMITTED_KEY);


    Id SUMMARY_EXP =  new Id(IdConstants.SEATA_EXCEPTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_SUMMARY);
}
