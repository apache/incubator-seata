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

import org.apache.seata.metrics.Id;
import org.apache.seata.metrics.IdConstants;

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

    Id TIMER_ROLLBACKED = new Id(IdConstants.SEATA_TRANSACTION)
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

    Id SUMMARY_COMMIT_RETRYING = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_SUMMARY)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_COMMIT_RETRYING_KEY);

    Id SUMMARY_ROLLBACK_RETRYING = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_SUMMARY)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_ROLLBACK_RETRYING_KEY);

    Id SUMMARY_TIMEOUT_ROLLBACK_RETRYING = new Id(IdConstants.SEATA_TRANSACTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_SUMMARY)
            .withTag(IdConstants.STATUS_KEY, IdConstants.STATUS_VALUE_TIMEOUT_ROLLBACK_RETRYING_KEY);

    Id SUMMARY_EXP = new Id(IdConstants.SEATA_EXCEPTION)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_SUMMARY);

    Id SUMMARY_RATE_LIMIT = new Id(IdConstants.SEATA_RATE_LIMIT)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_SUMMARY);

    String SEATA_RECOVERY_PIPELINE = "seata.recovery.pipeline";

    Id COUNTER_PIPELINE_ENQUEUED = new Id(SEATA_RECOVERY_PIPELINE)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.NAME_KEY, "enqueued");

    Id COUNTER_PIPELINE_DROPPED = new Id(SEATA_RECOVERY_PIPELINE)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.NAME_KEY, "dropped");

    Id COUNTER_PIPELINE_DEDUP_SKIPPED = new Id(SEATA_RECOVERY_PIPELINE)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.NAME_KEY, "dedupSkipped");

    Id COUNTER_PIPELINE_RPC_COMPLETED = new Id(SEATA_RECOVERY_PIPELINE)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.NAME_KEY, "rpcCompleted");

    Id COUNTER_PIPELINE_RPC_RETRY_NEEDED = new Id(SEATA_RECOVERY_PIPELINE)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.NAME_KEY, "rpcRetryNeeded");

    Id COUNTER_PIPELINE_RPC_FAILURE = new Id(SEATA_RECOVERY_PIPELINE)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.NAME_KEY, "rpcFailure");

    Id COUNTER_PIPELINE_RM_THROTTLED = new Id(SEATA_RECOVERY_PIPELINE)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.NAME_KEY, "rmThrottled");

    Id TIMER_PIPELINE_RPC_LATENCY = new Id(SEATA_RECOVERY_PIPELINE)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.NAME_KEY, "rpcLatency");

    Id COUNTER_PIPELINE_CLEANER_BATCHES = new Id(SEATA_RECOVERY_PIPELINE)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.NAME_KEY, "cleanerBatches");

    Id COUNTER_PIPELINE_CLEANER_DELETED_SESSIONS = new Id(SEATA_RECOVERY_PIPELINE)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.NAME_KEY, "cleanerDeletedSessions");

    Id COUNTER_PIPELINE_CLEANER_BATCH_FAILURES = new Id(SEATA_RECOVERY_PIPELINE)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.NAME_KEY, "cleanerBatchFailures");

    Id COUNTER_PIPELINE_CLEANER_DROPPED = new Id(SEATA_RECOVERY_PIPELINE)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag(IdConstants.NAME_KEY, "cleanerDropped");

    Id TIMER_PIPELINE_CLEANER_FLUSH_LATENCY = new Id(SEATA_RECOVERY_PIPELINE)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.NAME_KEY, "cleanerFlushLatency");

    Id TIMER_PIPELINE_DB_GUARD_WAIT = new Id(SEATA_RECOVERY_PIPELINE)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag(IdConstants.NAME_KEY, "dbGuardWait");

    Id GAUGE_PIPELINE_QUEUE_DEPTH = new Id(SEATA_RECOVERY_PIPELINE)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_GAUGE)
            .withTag(IdConstants.NAME_KEY, "queueDepth");

    Id GAUGE_PIPELINE_CLEANER_BUFFER_DEPTH = new Id(SEATA_RECOVERY_PIPELINE)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_GAUGE)
            .withTag(IdConstants.NAME_KEY, "cleanerBufferDepth");

    Id GAUGE_PIPELINE_INFLIGHT_COUNT = new Id(SEATA_RECOVERY_PIPELINE)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_GAUGE)
            .withTag(IdConstants.NAME_KEY, "inFlightCount");

    Id GAUGE_PIPELINE_DB_GUARD_ACTIVE = new Id(SEATA_RECOVERY_PIPELINE)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_GAUGE)
            .withTag(IdConstants.NAME_KEY, "dbGuardActive");

    Id GAUGE_PIPELINE_DB_GUARD_WAITING = new Id(SEATA_RECOVERY_PIPELINE)
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_GAUGE)
            .withTag(IdConstants.NAME_KEY, "dbGuardWaiting");
}
