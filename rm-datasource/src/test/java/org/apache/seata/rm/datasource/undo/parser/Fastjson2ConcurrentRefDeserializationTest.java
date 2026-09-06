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
package org.apache.seata.rm.datasource.undo.parser;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.BranchUndoLog;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.rm.datasource.undo.UndoLogParser;
import org.apache.seata.sqlparser.SQLType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Fastjson2ConcurrentRefDeserializationTest {

    private static final long CONCURRENT_TEST_TIMEOUT_SECONDS = 30;

    @Test
    public void concurrentDeserializeReferenceHeavyUndoLogDoesNotDropRefFields() throws Exception {
        int rounds = Integer.getInteger("seata.fastjson2.concurrentRef.rounds", 3);
        for (int round = 0; round < rounds; round++) {
            assertChildProcessSucceeds();
        }
    }

    public static void main(String[] args) throws Exception {
        Fastjson2UndoLogParser parser =
                (Fastjson2UndoLogParser) EnhancedServiceLoader.load(UndoLogParser.class, Fastjson2UndoLogParser.NAME);
        byte[] bytes = parser.encode(referenceHeavyUndoLog());

        int nullTasks =
                runConcurrentStress(parser, bytes, Integer.getInteger("seata.fastjson2.concurrentRef.threads", 200));
        if (nullTasks > 0) {
            throw new AssertionError("Concurrent deserialization dropped $ref fields: " + nullTasks);
        }
    }

    private static BranchUndoLog referenceHeavyUndoLog() {
        BranchUndoLog branchUndoLog = new BranchUndoLog();
        branchUndoLog.setXid("127.0.0.1:8091:123456");
        branchUndoLog.setBranchId(123456L);

        TableRecords sharedImage = tableRecords();
        List<SQLUndoLog> sqlUndoLogs = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            SQLUndoLog sqlUndoLog = new SQLUndoLog();
            sqlUndoLog.setSqlType(SQLType.UPDATE);
            sqlUndoLog.setTableName("ref_test");
            sqlUndoLog.setBeforeImage(sharedImage);
            sqlUndoLog.setAfterImage(sharedImage);
            sqlUndoLogs.add(sqlUndoLog);
        }
        branchUndoLog.setSqlUndoLogs(sqlUndoLogs);
        return branchUndoLog;
    }

    private static TableRecords tableRecords() {
        TableRecords tableRecords = new TableRecords();
        tableRecords.setTableName("ref_test");
        List<Row> rows = new ArrayList<>();
        Row row = new Row();
        row.add(new Field("id", Types.INTEGER, 1));
        row.add(new Field("name", Types.VARCHAR, "seata"));
        rows.add(row);
        tableRecords.setRows(rows);
        return tableRecords;
    }

    private static int countNullRefFields(BranchUndoLog branchUndoLog) {
        if (branchUndoLog == null || branchUndoLog.getSqlUndoLogs() == null) {
            return 1;
        }
        int nullCount = 0;
        for (SQLUndoLog sqlUndoLog : branchUndoLog.getSqlUndoLogs()) {
            if (sqlUndoLog == null) {
                nullCount++;
                continue;
            }
            if (sqlUndoLog.getBeforeImage() == null
                    || sqlUndoLog.getBeforeImage().getRows() == null) {
                nullCount++;
            }
            if (sqlUndoLog.getAfterImage() == null || sqlUndoLog.getAfterImage().getRows() == null) {
                nullCount++;
            }
        }
        return nullCount;
    }

    private static int runConcurrentStress(Fastjson2UndoLogParser parser, byte[] bytes, int threadCount)
            throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();

        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger nullTasks = new AtomicInteger();
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(
                    () -> {
                        try {
                            barrier.await(CONCURRENT_TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                            if (countNullRefFields(parser.decode(bytes)) > 0) {
                                nullTasks.incrementAndGet();
                            }
                        } catch (Throwable throwable) {
                            failure.compareAndSet(null, throwable);
                        } finally {
                            endLatch.countDown();
                        }
                    },
                    "fastjson2-undolog-ref-" + i);
            thread.start();
        }
        if (!endLatch.await(CONCURRENT_TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new AssertionError("Timed out waiting for concurrent deserialization");
        }
        if (failure.get() != null) {
            throw new AssertionError("Concurrent deserialization failed", failure.get());
        }

        return nullTasks.get();
    }

    private static void assertChildProcessSucceeds() throws Exception {
        Process process = new ProcessBuilder(
                        System.getProperty("java.home") + "/bin/java",
                        "-cp",
                        System.getProperty("java.class.path"),
                        Fastjson2ConcurrentRefDeserializationTest.class.getName())
                .redirectErrorStream(true)
                .start();
        if (!process.waitFor(CONCURRENT_TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("Timed out waiting for child process");
        }
        if (process.exitValue() != 0) {
            throw new AssertionError("Child process failed: " + readOutput(process.getInputStream()));
        }
    }

    private static String readOutput(InputStream inputStream) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, length);
        }
        return output.toString("UTF-8");
    }
}
