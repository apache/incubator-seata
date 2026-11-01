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

import com.esotericsoftware.kryo.Serializer;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.rm.datasource.undo.BaseUndoLogParserTest;
import org.apache.seata.rm.datasource.undo.UndoLogParser;
import org.apache.seata.rm.datasource.undo.parser.spi.KryoTypeSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

class KryoUndoLogParserTest extends BaseUndoLogParserTest {

    KryoUndoLogParser parser =
            (KryoUndoLogParser) EnhancedServiceLoader.load(UndoLogParser.class, KryoUndoLogParser.NAME);

    @Override
    public UndoLogParser getParser() {
        return parser;
    }

    static Stream<Object> columnValues() {
        return Stream.of(
                new java.sql.Date(-86_400_000L * 365),
                new java.sql.Time(45_296_000L),
                new java.util.Date(1_700_000_000_123L),
                Timestamp.valueOf("1960-01-01 00:00:00.123456789"),
                new BigDecimal("12345.6700"),
                new BigInteger("18446744073709551615"),
                Long.MIN_VALUE,
                (short) 7,
                Boolean.TRUE,
                "中文 and text",
                LocalDateTime.of(2024, 3, 2, 12, 34, 56, 123456789),
                LocalDate.of(1965, 1, 2),
                LocalTime.of(23, 59, 58, 1),
                OffsetDateTime.of(2024, 3, 2, 12, 0, 0, 0, ZoneOffset.ofHours(8)));
    }

    @ParameterizedTest
    @MethodSource("columnValues")
    void columnValueKeepsItsTypeAndValue(Object value) {
        Object decoded = UndoLogParserTestUtils.roundTripValue(parser, Types.OTHER, value);

        Assertions.assertEquals(value.getClass(), decoded.getClass());
        Assertions.assertEquals(value, decoded);
    }

    @Test
    void byteArrayKeepsItsContent() {
        byte[] bytes = "中文".getBytes(StandardCharsets.UTF_8);

        Object decoded = UndoLogParserTestUtils.roundTripValue(parser, Types.VARBINARY, bytes);

        Assertions.assertArrayEquals(bytes, (byte[]) decoded);
    }

    @Test
    void nullValueStaysNull() {
        Assertions.assertNull(UndoLogParserTestUtils.roundTripValue(parser, Types.TIMESTAMP, null));
    }

    @Test
    void concurrentEncodeAndDecodeKeepValuesApart() throws Exception {
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                int threadIndex = t;
                futures.add(executor.submit(() -> {
                    for (int i = 0; i < 200; i++) {
                        String value = "thread-" + threadIndex + "-" + i;
                        Assertions.assertEquals(
                                value, UndoLogParserTestUtils.roundTripValue(parser, Types.VARCHAR, value));
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void initRegistersSerializersLoadedFromSpi() {
        Serializer<?> serializer = Mockito.mock(Serializer.class);
        KryoTypeSerializer<BigDecimal> typed = typeSerializer(BigDecimal.class, serializer);
        KryoTypeSerializer<Object> withoutType = typeSerializer(null, serializer);
        KryoSerializerFactory factory = Mockito.mock(KryoSerializerFactory.class);

        try (MockedStatic<EnhancedServiceLoader> loader = Mockito.mockStatic(EnhancedServiceLoader.class);
                MockedStatic<KryoSerializerFactory> factoryHolder = Mockito.mockStatic(KryoSerializerFactory.class)) {
            loader.when(() -> EnhancedServiceLoader.loadAll(KryoTypeSerializer.class))
                    .thenReturn(Arrays.asList(typed, null, withoutType));
            factoryHolder.when(KryoSerializerFactory::getInstance).thenReturn(factory);

            new KryoUndoLogParser().init();
        }

        Mockito.verify(factory).registerSerializer(BigDecimal.class, serializer);
        Mockito.verifyNoMoreInteractions(factory);
    }

    @Test
    void initToleratesMissingSpiImplementations() {
        try (MockedStatic<EnhancedServiceLoader> loader = Mockito.mockStatic(EnhancedServiceLoader.class)) {
            loader.when(() -> EnhancedServiceLoader.loadAll(KryoTypeSerializer.class))
                    .thenThrow(new EnhancedServiceNotFoundException("not found"));

            Assertions.assertDoesNotThrow(() -> new KryoUndoLogParser().init());
        }
    }

    private static <T> KryoTypeSerializer<T> typeSerializer(Class<T> type, Serializer<?> serializer) {
        return new KryoTypeSerializer<T>() {
            @Override
            public Class<T> type() {
                return type;
            }

            @Override
            public Serializer serializer() {
                return serializer;
            }
        };
    }
}
