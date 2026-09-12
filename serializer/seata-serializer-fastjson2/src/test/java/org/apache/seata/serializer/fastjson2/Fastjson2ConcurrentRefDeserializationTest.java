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
package org.apache.seata.serializer.fastjson2;

import org.apache.seata.common.executor.Initialize;
import org.apache.seata.core.protocol.AbstractMessage;
import org.apache.seata.core.protocol.BatchResultMessage;
import org.apache.seata.core.protocol.MergedWarpMessage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
    public void concurrentDeserializeReferenceHeavyProtocolMessageDoesNotDropRefFields() throws Exception {
        int rounds = Integer.getInteger("seata.fastjson2.concurrentRef.rounds", 3);
        for (int round = 0; round < rounds; round++) {
            assertChildProcessSucceeds();
        }
    }

    public static void main(String[] args) throws Exception {
        Fastjson2Serializer serializer = new Fastjson2Serializer();
        ((Initialize) serializer).init();
        byte[] bytes = serializer.serialize(referenceHeavyMessage());

        int nullTasks = runConcurrentStress(
                serializer, bytes, Integer.getInteger("seata.fastjson2.concurrentRef.threads", 200));
        if (nullTasks > 0) {
            throw new AssertionError("Concurrent deserialization dropped $ref fields: " + nullTasks);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static MergedWarpMessage referenceHeavyMessage() {
        MergedWarpMessage message = new MergedWarpMessage();
        List sharedList = new ArrayList();
        for (int i = 0; i < 20; i++) {
            BatchResultMessage resultMessage = new BatchResultMessage();
            resultMessage.setResultMessages(sharedList);
            resultMessage.setMsgIds(sharedList);
            message.msgs.add(resultMessage);
            message.msgIds.add(i);
        }
        return message;
    }

    private static int countNullRefFields(MergedWarpMessage message) {
        if (message == null || message.msgs == null) {
            return 1;
        }
        int nullCount = 0;
        for (AbstractMessage child : message.msgs) {
            if (!(child instanceof BatchResultMessage)) {
                nullCount++;
                continue;
            }
            BatchResultMessage batchResult = (BatchResultMessage) child;
            if (batchResult.getResultMessages() == null) {
                nullCount++;
            }
            if (batchResult.getMsgIds() == null) {
                nullCount++;
            }
        }
        return nullCount;
    }

    private static int runConcurrentStress(Fastjson2Serializer serializer, byte[] bytes, int threadCount)
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
                            if (countNullRefFields((MergedWarpMessage) serializer.deserialize(bytes)) > 0) {
                                nullTasks.incrementAndGet();
                            }
                        } catch (Throwable throwable) {
                            failure.compareAndSet(null, throwable);
                        } finally {
                            endLatch.countDown();
                        }
                    },
                    "fastjson2-rpc-ref-" + i);
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
