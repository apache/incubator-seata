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
package org.apache.seata.common.json;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Fastjson2JsonbParserTest {

    @Test
    public void concurrentEntryPointsPreserveReferences() throws Exception {
        byte[] bytes = payload();
        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 16; i++) {
                boolean filtered = i % 2 == 0;
                futures.add(executor.submit(() -> {
                    assertTrue(start.await(30, TimeUnit.SECONDS));
                    for (int j = 0; j < 25; j++) {
                        SharedReferences value = filtered
                                ? (SharedReferences) Fastjson2JsonbParser.parseObject(
                                        bytes,
                                        Object.class,
                                        JSONReader.autoTypeFilter(SharedReferences.class),
                                        JSONReader.Feature.FieldBased)
                                : Fastjson2JsonbParser.parseObject(
                                        bytes, SharedReferences.class, JSONReader.Feature.FieldBased);
                        assertReferences(value);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        }
    }

    @Test
    public void malformedInputDoesNotPreventSubsequentDecoding() throws Exception {
        assertThrows(
                RuntimeException.class,
                () -> Fastjson2JsonbParser.parseObject(
                        new byte[0], SharedReferences.class, JSONReader.Feature.FieldBased));
        // Use a different thread so a leaked lock cannot be hidden by reentrancy.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            SharedReferences value = executor.submit(() -> Fastjson2JsonbParser.parseObject(
                            payload(), SharedReferences.class, JSONReader.Feature.FieldBased))
                    .get(30, TimeUnit.SECONDS);
            assertReferences(value);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        }
    }

    private static byte[] payload() {
        SharedReferences value = new SharedReferences();
        value.first = new ArrayList<>(Arrays.asList("seata"));
        value.second = value.first;
        return JSONB.toBytes(
                value,
                JSONWriter.Feature.FieldBased,
                JSONWriter.Feature.WriteClassName,
                JSONWriter.Feature.ReferenceDetection);
    }

    private static void assertReferences(SharedReferences value) {
        assertEquals(Arrays.asList("seata"), value.first);
        assertSame(value.first, value.second);
    }

    public static class SharedReferences {
        private List<String> first;
        private List<String> second;
    }
}
