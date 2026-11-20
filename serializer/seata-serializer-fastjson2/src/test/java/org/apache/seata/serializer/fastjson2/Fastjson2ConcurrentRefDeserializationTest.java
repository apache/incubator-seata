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

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.writer.ObjectWriterProvider;
import org.apache.seata.common.executor.Initialize;
import org.apache.seata.core.protocol.AbstractMessage;
import org.apache.seata.core.protocol.BatchResultMessage;
import org.apache.seata.core.protocol.MergedWarpMessage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Fastjson2ConcurrentRefDeserializationTest {

    private static final long CONCURRENT_TEST_TIMEOUT_SECONDS = 30;

    // JSONB produced by fastjson2 2.0.52 with ReferenceDetection; first shared list path is
    // $.msgs[0].msgIds. Use a fixed payload to isolate decoding from serializer issue #8331.
    private static final String REFERENCE_HEAVY_JSONB =
            "knk4MG9yZy5hcGFjaGUuc2VhdGEuY29yZS5wcm90b2NvbC5NZXJnZWRXYXJwTWVzc2FnZQCmf0xjdHgBr39PbXNnSWRzAqQUAAEC"
                    + "AwQFBgcICQoLDA0ODxAREhN/TW1zZ3MDpBSSeTgxb3JnLmFwYWNoZS5zZWF0YS5jb3JlLnByb3RvY29sLkJhdGNoUmVzdWx0TWVz"
                    + "c2FnZQSmfwGvfwKUf1dyZXN1bHRNZXNzYWdlcwWTWSQubXNnc1swXS5tc2dJZHN/Ba+lkgSmfwGvfwKTTCMtMX8Cr38Fk0wjLTF/"
                    + "Ba+lkgSmfwGvfwKTTCMtMX8Cr38Fk0wjLTF/Ba+lkgSmfwGvfwKTTCMtMX8Cr38Fk0wjLTF/Ba+lkgSmfwGvfwKTTCMtMX8Cr38F"
                    + "k0wjLTF/Ba+lkgSmfwGvfwKTTCMtMX8Cr38Fk0wjLTF/Ba+lkgSmfwGvfwKTTCMtMX8Cr38Fk0wjLTF/Ba+lkgSmfwGvfwKTTCMt"
                    + "MX8Cr38Fk0wjLTF/Ba+lkgSmfwGvfwKTTCMtMX8Cr38Fk0wjLTF/Ba+lkgSmfwGvfwKTTCMtMX8Cr38Fk0wjLTF/Ba+lkgSmfwGv"
                    + "fwKTTCMtMX8Cr38Fk0wjLTF/Ba+lkgSmfwGvfwKTTCMtMX8Cr38Fk0wjLTF/Ba+lkgSmfwGvfwKTTCMtMX8Cr38Fk0wjLTF/Ba+l"
                    + "kgSmfwGvfwKTTCMtMX8Cr38Fk0wjLTF/Ba+lkgSmfwGvfwKTTCMtMX8Cr38Fk0wjLTF/Ba+lkgSmfwGvfwKTTCMtMX8Cr38Fk0wj"
                    + "LTF/Ba+lkgSmfwGvfwKTTCMtMX8Cr38Fk0wjLTF/Ba+lkgSmfwGvfwKTTCMtMX8Cr38Fk0wjLTF/Ba+lkgSmfwGvfwKTTCMtMX8C"
                    + "r38Fk0wjLTF/Ba+lkgSmfwGvfwKTTCMtMX8Cr38Fk0wjLTF/Ba+lpQ==";

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
        Fastjson2Serializer otherSerializer = new Fastjson2Serializer();
        otherSerializer.init();
        Fastjson2Serializer[] serializers = {serializer, otherSerializer};
        byte[] bytes = Base64.getDecoder().decode(REFERENCE_HEAVY_JSONB);

        int invalidTasks = runConcurrentStress(
                serializers, bytes, Integer.getInteger("seata.fastjson2.concurrentRef.threads", 200));
        if (invalidTasks > 0) {
            MergedWarpMessage sequential = serializer.deserialize(bytes);
            int sequentialInvalid = countInvalidRefFields(sequential);
            BatchResultMessage first = (BatchResultMessage) sequential.msgs.get(0);
            throw new AssertionError("Concurrent deserialization lost reference identity: " + invalidTasks
                    + "; sequential invalid fields: " + sequentialInvalid
                    + "; first msgIds=" + first.getMsgIds()
                    + "; first resultMessages=" + first.getResultMessages()
                    + "; same instance=" + (first.getMsgIds() == (Object) first.getResultMessages())
                    + "; fastjson=" + JSON.VERSION
                    + "; batchReferenceDetectionDisabled="
                    + ObjectWriterProvider.isNotReferenceDetect(BatchResultMessage.class)
                    + "; java=" + System.getProperty("java.runtime.version")
                    + "; vendor=" + System.getProperty("java.vendor")
                    + "; JSONB=" + JSONB.toJSONString(bytes));
        }
    }

    private static int countInvalidRefFields(MergedWarpMessage message) {
        if (message == null || message.msgs == null) {
            return 1;
        }
        int invalidCount = 0;
        Object sharedList = ((BatchResultMessage) message.msgs.get(0)).getResultMessages();
        for (AbstractMessage child : message.msgs) {
            if (!(child instanceof BatchResultMessage)) {
                invalidCount++;
                continue;
            }
            BatchResultMessage batchResult = (BatchResultMessage) child;
            if (batchResult.getResultMessages() == null || batchResult.getResultMessages() != sharedList) {
                invalidCount++;
            }
            if (batchResult.getMsgIds() == null || batchResult.getMsgIds() != sharedList) {
                invalidCount++;
            }
        }
        return invalidCount;
    }

    private static int runConcurrentStress(Fastjson2Serializer[] serializers, byte[] bytes, int threadCount)
            throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();

        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger invalidTasks = new AtomicInteger();
        for (int i = 0; i < threadCount; i++) {
            Fastjson2Serializer serializer = serializers[i % serializers.length];
            Thread thread = new Thread(
                    () -> {
                        try {
                            barrier.await(CONCURRENT_TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                            if (countInvalidRefFields((MergedWarpMessage) serializer.deserialize(bytes)) > 0) {
                                invalidTasks.incrementAndGet();
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

        return invalidTasks.get();
    }

    private static void assertChildProcessSucceeds() throws Exception {
        Process process = new ProcessBuilder(
                        System.getProperty("java.home") + "/bin/java",
                        "-Dseata.fastjson2.concurrentRef.threads="
                                + Integer.getInteger("seata.fastjson2.concurrentRef.threads", 200),
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
