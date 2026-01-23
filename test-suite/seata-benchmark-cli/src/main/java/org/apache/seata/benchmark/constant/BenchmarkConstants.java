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
package org.apache.seata.benchmark.constant;

/**
 * Benchmark constants
 */
public final class BenchmarkConstants {

    private BenchmarkConstants() {
        // Prevent instantiation
    }

    // Server configuration
    public static final String DEFAULT_SERVER_ADDRESS = "127.0.0.1:8091";
    public static final String DEFAULT_TX_GROUP = "default_tx_group";

    // Transaction configuration
    public static final int TRANSACTION_TIMEOUT_MS = 30000;

    // Workload generator configuration
    public static final int MAX_RECENT_RECORDS = 10;
    public static final int PAUSE_CHECK_INTERVAL_MS = 100;

    // Real AT mode configuration
    public static final int ACCOUNT_COUNT = 1000;
    public static final int INITIAL_BALANCE = 10000;
    public static final int MIN_TRANSFER_AMOUNT = 1;
    public static final int MAX_TRANSFER_AMOUNT = 100;

    // Thread pool configuration
    public static final int SHUTDOWN_TIMEOUT_SECONDS = 10;

    // Progress reporting interval
    public static final int PROGRESS_REPORT_INTERVAL_SECONDS = 10;

    // Latency sampling configuration
    public static final int MAX_LATENCY_SAMPLES = 500000;
    public static final long LATENCY_STATS_CACHE_MS = 1000;

    // TPS configuration
    public static final int UNLIMITED_TPS_THRESHOLD = 10000;

    // Transaction status constants for benchmark reporting
    public static final String STATUS_COMMITTED = "Committed";
    public static final String STATUS_ROLLBACKED = "Rollbacked";
    public static final String STATUS_COMPENSATED = "Compensated";
    public static final String STATUS_COMPENSATION_FAILED = "CompensationFailed";
    public static final String STATUS_FAILED = "Failed";
    public static final String STATUS_UNKNOWN = "Unknown";
}
