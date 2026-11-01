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

import io.protostuff.Input;
import io.protostuff.Output;
import io.protostuff.Pipe;
import io.protostuff.runtime.Delegate;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.rm.datasource.undo.BaseUndoLogParserTest;
import org.apache.seata.rm.datasource.undo.BranchUndoLog;
import org.apache.seata.rm.datasource.undo.UndoLogParser;
import org.apache.seata.rm.datasource.undo.parser.spi.ProtostuffDelegate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.stream.Stream;

class ProtostuffUndoLogParserTest extends BaseUndoLogParserTest {

    ProtostuffUndoLogParser parser =
            (ProtostuffUndoLogParser) EnhancedServiceLoader.load(UndoLogParser.class, ProtostuffUndoLogParser.NAME);

    @Override
    public UndoLogParser getParser() {
        return parser;
    }

    static Stream<Object> columnValues() {
        return Stream.of(
                new java.sql.Date(-86_400_000L * 365),
                new java.sql.Time(45_296_000L),
                new java.util.Date(1_700_000_000_123L),
                new BigDecimal("12345.6700"),
                new BigInteger("18446744073709551615"),
                Long.MIN_VALUE,
                (short) 7,
                Boolean.TRUE,
                "中文 and text",
                LocalDateTime.of(2024, 3, 2, 12, 34, 56, 123456789),
                LocalDate.of(1965, 1, 2),
                LocalTime.of(23, 59, 58, 1));
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
    void timestampBeforeEpochKeepsNanos() {
        Timestamp timestamp = Timestamp.valueOf("1960-01-01 00:00:00.123456789");

        Object decoded = UndoLogParserTestUtils.roundTripValue(parser, Types.TIMESTAMP, timestamp);

        Assertions.assertEquals(timestamp, decoded);
        Assertions.assertEquals(123456789, ((Timestamp) decoded).getNanos());
    }

    @Test
    void nullValueStaysNull() {
        Assertions.assertNull(UndoLogParserTestUtils.roundTripValue(parser, Types.TIMESTAMP, null));
    }

    @Test
    void decodeEmptyBytesReturnsEmptyUndoLog() {
        BranchUndoLog branchUndoLog = parser.decode(new byte[0]);

        Assertions.assertNull(branchUndoLog.getXid());
        Assertions.assertNull(branchUndoLog.getSqlUndoLogs());
    }

    @Test
    void dateDelegatesTransferFixed64() throws IOException {
        Pipe pipe = Mockito.mock(Pipe.class);
        Input input = Mockito.mock(Input.class);
        Output output = Mockito.mock(Output.class);
        Mockito.when(input.readFixed64()).thenReturn(1L, 2L, 3L);

        new ProtostuffUndoLogParser.DateDelegate().transfer(pipe, input, output, 1, false);
        new ProtostuffUndoLogParser.SqlDateDelegate().transfer(pipe, input, output, 2, false);
        new ProtostuffUndoLogParser.TimeDelegate().transfer(pipe, input, output, 3, true);

        Mockito.verify(output).writeFixed64(1, 1L, false);
        Mockito.verify(output).writeFixed64(2, 2L, false);
        Mockito.verify(output).writeFixed64(3, 3L, true);
    }

    @Test
    void timestampDelegateTransfersBytes() throws IOException {
        Pipe pipe = Mockito.mock(Pipe.class);
        Input input = Mockito.mock(Input.class);
        Output output = Mockito.mock(Output.class);
        ByteBuffer buffer = ByteBuffer.allocate(12);
        Mockito.when(input.readByteBuffer()).thenReturn(buffer);

        new ProtostuffUndoLogParser.TimestampDelegate().transfer(pipe, input, output, 4, false);

        Mockito.verify(output).writeBytes(4, buffer, false);
    }

    @Test
    @SuppressWarnings("unchecked")
    void initRegistersDelegatesLoadedFromSpi() {
        Delegate<SpiOnlyType> delegate = Mockito.mock(Delegate.class);
        Mockito.when(delegate.typeClass()).thenReturn((Class) SpiOnlyType.class);
        ProtostuffDelegate<SpiOnlyType> spi = Mockito.mock(ProtostuffDelegate.class);
        Mockito.when(spi.create()).thenReturn(delegate);

        try (MockedStatic<EnhancedServiceLoader> loader = Mockito.mockStatic(EnhancedServiceLoader.class)) {
            loader.when(() -> EnhancedServiceLoader.loadAll(ProtostuffDelegate.class))
                    .thenReturn(Collections.singletonList(spi));

            new ProtostuffUndoLogParser().init();
        }

        Mockito.verify(spi).create();
        Mockito.verify(delegate, Mockito.atLeastOnce()).typeClass();
    }

    @Test
    void initToleratesMissingSpiImplementations() {
        try (MockedStatic<EnhancedServiceLoader> loader = Mockito.mockStatic(EnhancedServiceLoader.class)) {
            loader.when(() -> EnhancedServiceLoader.loadAll(ProtostuffDelegate.class))
                    .thenThrow(new EnhancedServiceNotFoundException("not found"));

            Assertions.assertDoesNotThrow(() -> new ProtostuffUndoLogParser().init());
        }
    }

    /** Only used as the type of the delegate registered through the mocked SPI. */
    static class SpiOnlyType {}
}
