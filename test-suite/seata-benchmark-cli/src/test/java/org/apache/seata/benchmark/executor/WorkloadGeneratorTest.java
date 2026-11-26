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
package org.apache.seata.benchmark.executor;

import org.apache.seata.benchmark.config.BenchmarkConfig;
import org.apache.seata.benchmark.model.BenchmarkMetrics;
import org.apache.seata.benchmark.model.TransactionRecord;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.seata.benchmark.constant.BenchmarkConstants.STATUS_COMMITTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkloadGeneratorTest {

    @Test
    void doesNotStartTransactionWhenPermitArrivesAtDeadline() throws InterruptedException {
        BenchmarkConfig config = new BenchmarkConfig();
        config.setDuration(1);
        config.setThreads(1);

        BenchmarkMetrics metrics = new BenchmarkMetrics();
        AtomicLong clock = new AtomicLong();
        AtomicInteger permits = new AtomicInteger();
        CountDownLatch deadlineReached = new CountDownLatch(1);
        TransactionExecutor executor = new TransactionExecutor() {
            @Override
            public TransactionRecord execute() {
                return new TransactionRecord("xid", STATUS_COMMITTED, 1, 1, true);
            }

            @Override
            public void init() {}

            @Override
            public void destroy() {}
        };

        WorkloadGenerator generator = new WorkloadGenerator(
                config,
                executor,
                metrics,
                () -> {
                    if (permits.incrementAndGet() == 2) {
                        clock.set(1000);
                    }
                },
                () -> {
                    long now = clock.get();
                    if (now == 1000) {
                        deadlineReached.countDown();
                    }
                    return now;
                },
                Executors.newSingleThreadExecutor());

        generator.start();
        try {
            assertTrue(deadlineReached.await(5, TimeUnit.SECONDS), "The worker did not reach the deadline");
        } finally {
            generator.stop();
        }
        assertEquals(2, permits.get());
        assertEquals(1, metrics.getTotalCount());
    }
}
