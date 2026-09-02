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
package org.apache.seata.server.storage.rocksdb.maintenance;

/**
 * Immutable options for consistency verification.
 */
public final class RocksDBVerifyOptions {

    private static final int DEFAULT_MAX_ERROR_SAMPLES = 100;
    private static final long DEFAULT_DEADLINE_MILLIS = 60_000L;

    private final RocksDBVerifyMode mode;
    private final int limit;
    private final RocksDBVerifyCursor cursor;
    private final int maxErrorSamples;
    private final long deadlineMillis;

    private RocksDBVerifyOptions(
            RocksDBVerifyMode mode, int limit, RocksDBVerifyCursor cursor, int maxErrorSamples, long deadlineMillis) {
        if (mode != RocksDBVerifyMode.FULL && limit <= 0) {
            throw new IllegalArgumentException("verify limit must be positive for " + mode);
        }
        if (maxErrorSamples < 0) {
            throw new IllegalArgumentException("maxErrorSamples must be non-negative");
        }
        if (mode != RocksDBVerifyMode.PAGE && cursor != null) {
            throw new IllegalArgumentException("verify cursor is only supported in PAGE mode");
        }
        if (deadlineMillis < 0) {
            throw new IllegalArgumentException("verify deadlineMillis must be non-negative");
        }
        this.mode = mode;
        this.limit = limit;
        this.cursor = cursor;
        this.maxErrorSamples = maxErrorSamples;
        this.deadlineMillis = deadlineMillis;
    }

    public static RocksDBVerifyOptions full() {
        return full(DEFAULT_MAX_ERROR_SAMPLES);
    }

    public static RocksDBVerifyOptions full(int maxErrorSamples) {
        return full(maxErrorSamples, DEFAULT_DEADLINE_MILLIS);
    }

    public static RocksDBVerifyOptions full(int maxErrorSamples, long deadlineMillis) {
        return new RocksDBVerifyOptions(RocksDBVerifyMode.FULL, 0, null, maxErrorSamples, deadlineMillis);
    }

    public static RocksDBVerifyOptions sample(int limitPerColumnFamily, int maxErrorSamples) {
        return sample(limitPerColumnFamily, maxErrorSamples, DEFAULT_DEADLINE_MILLIS);
    }

    public static RocksDBVerifyOptions sample(int limitPerColumnFamily, int maxErrorSamples, long deadlineMillis) {
        return new RocksDBVerifyOptions(
                RocksDBVerifyMode.SAMPLE, limitPerColumnFamily, null, maxErrorSamples, deadlineMillis);
    }

    public static RocksDBVerifyOptions page(int limit, RocksDBVerifyCursor cursor, int maxErrorSamples) {
        return page(limit, cursor, maxErrorSamples, DEFAULT_DEADLINE_MILLIS);
    }

    public static RocksDBVerifyOptions page(
            int limit, RocksDBVerifyCursor cursor, int maxErrorSamples, long deadlineMillis) {
        return new RocksDBVerifyOptions(RocksDBVerifyMode.PAGE, limit, cursor, maxErrorSamples, deadlineMillis);
    }

    public RocksDBVerifyMode getMode() {
        return mode;
    }

    public int getLimit() {
        return limit;
    }

    public RocksDBVerifyCursor getCursor() {
        return cursor;
    }

    public int getMaxErrorSamples() {
        return maxErrorSamples;
    }

    public long getDeadlineMillis() {
        return deadlineMillis;
    }
}
