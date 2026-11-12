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
package org.apache.seata.rm.datasource;

import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.undo.AbstractUndoLogManager;
import org.apache.seata.rm.datasource.undo.parser.FastjsonUndoLogParser;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataCompareUtilsBehaviorTest {
    static Stream<Arguments> values() {
        return Stream.of(
                Arguments.of(Types.DATE, "2024-03-02 00:00:00", Date.valueOf("2024-03-02")),
                Arguments.of(Types.TIME, "12:34:56", Time.valueOf("12:34:56")),
                Arguments.of(Types.TIMESTAMP, "2024-03-02 12:34:56.123", Timestamp.valueOf("2024-03-02 12:34:56.123")),
                Arguments.of(Types.DECIMAL, 42, new BigDecimal("42")),
                Arguments.of(Types.BIGINT, 42, 42L));
    }

    @ParameterizedTest
    @MethodSource("values")
    void fastjsonValuesCompareWithJdbcTypesInEitherDirection(int type, Object serialized, Object jdbc) {
        String previous = AbstractUndoLogManager.getCurrentSerializer();
        try {
            AbstractUndoLogManager.setCurrentSerializer(FastjsonUndoLogParser.NAME);
            assertTrue(
                    DataCompareUtils.isFieldEquals(new Field("value", type, serialized), new Field("VALUE", type, jdbc))
                            .getResult());
            assertTrue(
                    DataCompareUtils.isFieldEquals(new Field("value", type, jdbc), new Field("VALUE", type, serialized))
                            .getResult());
        } finally {
            if (previous == null) {
                AbstractUndoLogManager.removeCurrentSerializer();
            } else {
                AbstractUndoLogManager.setCurrentSerializer(previous);
            }
        }
    }

    @Test
    void fastjsonLocalDateTimeAndNullValues() {
        String previous = AbstractUndoLogManager.getCurrentSerializer();
        try {
            AbstractUndoLogManager.setCurrentSerializer(FastjsonUndoLogParser.NAME);
            assertTrue(DataCompareUtils.isFieldEquals(
                            new Field("time", Types.TIMESTAMP, "2024-03-02T12:34:56"),
                            new Field("time", Types.TIMESTAMP, LocalDateTime.parse("2024-03-02T12:34:56")))
                    .getResult());
            assertTrue(DataCompareUtils.isFieldEquals(null, null).getResult());
            assertFalse(DataCompareUtils.isFieldEquals(new Field("id", Types.INTEGER, 1), null)
                    .getResult());
            assertFalse(DataCompareUtils.isFieldEquals(
                            new Field("id", Types.INTEGER, 1), new Field("id", Types.INTEGER, null))
                    .getResult());
        } finally {
            if (previous == null) {
                AbstractUndoLogManager.removeCurrentSerializer();
            } else {
                AbstractUndoLogManager.setCurrentSerializer(previous);
            }
        }
    }

    @Test
    void rowsMatchByCompositeKeyAndReportMissingColumnsOrRows() {
        TableMeta meta = mock(TableMeta.class);
        when(meta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList("id", "tenant"));
        Row original = new Row();
        original.add(new Field("tenant", Types.INTEGER, 2));
        original.add(new Field("id", Types.INTEGER, 1));
        original.add(new Field("value", Types.VARCHAR, "before"));
        Row missing = new Row();
        missing.add(new Field("id", Types.INTEGER, 1));
        missing.add(new Field("tenant", Types.INTEGER, 2));
        assertFalse(DataCompareUtils.isRowsEquals(
                        meta, Collections.singletonList(original), Collections.singletonList(missing))
                .getResult());
        assertTrue(DataCompareUtils.rowListToMap(Collections.singletonList(original), Arrays.asList("id", "tenant"))
                .containsKey("1_2"));
        missing.getFields().get(0).setValue(3);
        assertFalse(DataCompareUtils.isRowsEquals(
                        meta, Collections.singletonList(original), Collections.singletonList(missing))
                .getResult());
    }
}
