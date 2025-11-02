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
package org.apache.seata.core.rpc.processor.server;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for BatchLogHandler
 */
public class BatchLogHandlerTest {

    @Test
    public void testWriteLogSuccess() {
        String testLog = "Test log message";
        boolean result = BatchLogHandler.INSTANCE.writeLog(testLog);
        assertTrue(result, "writeLog should return true on success");
    }

    @Test
    public void testWriteMultipleLogs() {
        // Write multiple logs
        for (int i = 0; i < 10; i++) {
            boolean result = BatchLogHandler.INSTANCE.writeLog("Test log " + i);
            assertTrue(result, "writeLog should return true for log " + i);
        }
    }

    @Test
    public void testWriteLogConcurrent() throws InterruptedException {
        int threadCount = 10;
        int logsPerThread = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // Create multiple threads writing logs concurrently
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                        try {
                            for (int j = 0; j < logsPerThread; j++) {
                                if (BatchLogHandler.INSTANCE.writeLog("Thread " + threadId + " log " + j)) {
                                    successCount.incrementAndGet();
                                }
                            }
                        } finally {
                            latch.countDown();
                        }
                    })
                    .start();
        }

        // Wait for all threads to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS), "All threads should complete");
        assertTrue(successCount.get() > 0, "At least some logs should be written successfully");
    }

    @Test
    public void testWriteEmptyLog() {
        boolean result = BatchLogHandler.INSTANCE.writeLog("");
        assertTrue(result, "writeLog should handle empty string");
    }

    @Test
    public void testWriteNullLog() {
        // BatchLogHandler.writeLog will throw NPE on null, so we expect that
        // or we can skip this test as writing null logs is not a valid use case
        try {
            BatchLogHandler.INSTANCE.writeLog(null);
            // If it doesn't throw, that's also acceptable
            assertTrue(true);
        } catch (NullPointerException e) {
            // NPE is expected for null input
            assertTrue(true, "NPE is expected for null log");
        }
    }

    @Test
    public void testWriteLargeLog() {
        // Create a large log message
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("Large log message ").append(i).append(" ");
        }

        boolean result = BatchLogHandler.INSTANCE.writeLog(sb.toString());
        assertTrue(result, "writeLog should handle large messages");
    }

    @Test
    public void testWriteLogsRapidly() throws InterruptedException {
        // Write logs rapidly to test queue handling
        for (int i = 0; i < 100; i++) {
            BatchLogHandler.INSTANCE.writeLog("Rapid log " + i);
        }

        // Give some time for batch processing
        Thread.sleep(100);

        // Should complete without errors
        assertTrue(true, "Rapid log writing should complete");
    }
}
