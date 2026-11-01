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

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONWriter;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.rm.datasource.DataCompareUtils;
import org.apache.seata.rm.datasource.sql.serial.SerialArray;
import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.BaseUndoLogParserTest;
import org.apache.seata.rm.datasource.undo.BranchUndoLog;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.rm.datasource.undo.UndoLogParser;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Array;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Fastjson2UndoLogParserTest extends BaseUndoLogParserTest {

    Fastjson2UndoLogParser parser =
            (Fastjson2UndoLogParser) EnhancedServiceLoader.load(UndoLogParser.class, Fastjson2UndoLogParser.NAME);

    @Override
    public UndoLogParser getParser() {
        return parser;
    }

    @Test
    public void testTimestampKeepsTypeAndNanos() {
        Timestamp timestamp = Timestamp.valueOf("2024-03-02 12:34:56.123456789");
        Field field = new Field("gmt_modified", Types.TIMESTAMP, timestamp);

        Field decodedField = getFieldFromLog(getParser().decode(getParser().encode(createTestUndoLog(field))));

        Assertions.assertEquals(Timestamp.class, decodedField.getValue().getClass());
        Assertions.assertEquals(timestamp, decodedField.getValue());
        Assertions.assertEquals(123456789, ((Timestamp) decodedField.getValue()).getNanos());
        Assertions.assertTrue(
                DataCompareUtils.isFieldEquals(field, decodedField).getResult());
    }

    @Test
    public void testTimestampBeforeEpoch() {
        Timestamp timestamp = new Timestamp(-1L);
        Field field = new Field("gmt_modified", Types.TIMESTAMP, timestamp);

        Field decodedField = getFieldFromLog(getParser().decode(getParser().encode(createTestUndoLog(field))));

        Assertions.assertEquals(timestamp, decodedField.getValue());
    }

    @Test
    public void testNullTimestamp() {
        Field field = new Field("gmt_modified", Types.TIMESTAMP, null);

        Field decodedField = getFieldFromLog(getParser().decode(getParser().encode(createTestUndoLog(field))));

        Assertions.assertNull(decodedField.getValue());
    }

    @Test
    public void testLocalDateTimeIsNotConvertedToTimestamp() {
        // e.g. MySQL Connector/J 8 returns LocalDateTime for DATETIME columns
        LocalDateTime localDateTime = LocalDateTime.of(2024, 3, 2, 12, 34, 56, 123456789);
        Field field = new Field("gmt_create", Types.TIMESTAMP, localDateTime);

        Field decodedField = getFieldFromLog(getParser().decode(getParser().encode(createTestUndoLog(field))));

        Assertions.assertEquals(localDateTime, decodedField.getValue());
    }

    @Test
    public void testDecodeUndoLogWrittenWithoutTimestampCodec() {
        Field field = new Field("gmt_modified", Types.TIMESTAMP, Timestamp.valueOf("2024-03-02 12:34:56.123456789"));
        byte[] previousFormat = JSONB.toBytes(
                createTestUndoLog(field),
                JSONWriter.Feature.WriteClassName,
                JSONWriter.Feature.FieldBased,
                JSONWriter.Feature.ReferenceDetection,
                JSONWriter.Feature.WriteNulls,
                JSONWriter.Feature.NotWriteDefaultValue,
                JSONWriter.Feature.NotWriteHashMapArrayListClassName,
                JSONWriter.Feature.WriteNameAsSymbol);

        Field decodedField = getFieldFromLog(getParser().decode(previousFormat));

        Assertions.assertEquals(LocalDateTime.of(2024, 3, 2, 12, 34, 56, 123456789), decodedField.getValue());
    }

    @Test
    public void testTimestampWriterIsNotRegisteredGlobally() {
        Assertions.assertFalse(
                JSONFactory.getDefaultObjectWriterProvider().getObjectWriter(Timestamp.class, Timestamp.class, true)
                        instanceof Fastjson2UndoLogParser.TimestampWriter);
    }

    @Test
    public void testSerializeAndDeserializeSerialArray() throws IOException, SQLException {
        // create a mock Array object for testing SerialArray
        Array mockArray = new MockArray();
        SerialArray serialArray = new SerialArray(mockArray);

        // test SerialArray with BIGINT array (PostgreSQL _int8 type)
        Field field = new Field("dept_ids", JDBCType.ARRAY.getVendorTypeNumber(), serialArray);
        byte[] bytes = getParser().encode(createTestUndoLog(field));

        BranchUndoLog decodedLog = getParser().decode(bytes);
        Field decodedField = getFieldFromLog(decodedLog);

        Assertions.assertTrue(
                DataCompareUtils.isFieldEquals(field, decodedField).getResult());

        // verify the SerialArray properties are correctly serialized/deserialized
        SerialArray deserializedArray = (SerialArray) decodedField.getValue();
        Assertions.assertEquals(serialArray.getBaseType(), deserializedArray.getBaseType());
        Assertions.assertEquals(serialArray.getBaseTypeName(), deserializedArray.getBaseTypeName());
        Assertions.assertArrayEquals(serialArray.getElements(), deserializedArray.getElements());
    }

    @Test
    public void testSerializeAndDeserializeSerialArrayWithNulls() throws IOException, SQLException {
        // create SerialArray with null elements
        Array mockArrayWithNulls = new MockArrayWithNulls();
        SerialArray serialArray = new SerialArray(mockArrayWithNulls);

        Field field = new Field("nullable_array", JDBCType.ARRAY.getVendorTypeNumber(), serialArray);
        byte[] bytes = getParser().encode(createTestUndoLog(field));
        BranchUndoLog decodedLog = getParser().decode(bytes);
        Field decodedField = getFieldFromLog(decodedLog);

        Assertions.assertTrue(
                DataCompareUtils.isFieldEquals(field, decodedField).getResult());

        // verify null elements are handled correctly
        SerialArray deserializedArray = (SerialArray) decodedField.getValue();
        Object[] elements = deserializedArray.getElements();
        Assertions.assertEquals(3, elements.length);
        Assertions.assertEquals(1L, elements[0]);
        Assertions.assertNull(elements[1]);
        Assertions.assertEquals(3L, elements[2]);
    }

    private BranchUndoLog createTestUndoLog(Field field) {
        BranchUndoLog branchUndoLog = new BranchUndoLog();
        branchUndoLog.setXid("192.168.0.1:8091:123456");
        branchUndoLog.setBranchId(123457);

        List<SQLUndoLog> sqlUndoLogs = new ArrayList<>();
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.UPDATE);
        sqlUndoLog.setTableName("test_table");

        // Create before image with the field
        TableRecords beforeImage = new TableRecords();
        List<Row> beforeRows = new ArrayList<>();
        Row beforeRow = new Row();
        beforeRow.add(field);
        beforeRows.add(beforeRow);
        beforeImage.setRows(beforeRows);
        beforeImage.setTableMeta(new TableMeta());
        beforeImage.setTableName("test_table");
        sqlUndoLog.setBeforeImage(beforeImage);

        // Create empty after image
        sqlUndoLog.setAfterImage(TableRecords.empty(new TableMeta()));

        sqlUndoLogs.add(sqlUndoLog);
        branchUndoLog.setSqlUndoLogs(sqlUndoLogs);

        return branchUndoLog;
    }

    private Field getFieldFromLog(BranchUndoLog undoLog) {
        return undoLog.getSqlUndoLogs()
                .get(0)
                .getBeforeImage()
                .getRows()
                .get(0)
                .getFields()
                .get(0);
    }

    /**
     * Mock Array class for testing SerialArray serialization
     */
    private static class MockArray implements Array {
        private final Object[] elements = {1L, 2L, 3L, 4L, 5L};

        @Override
        public String getBaseTypeName() throws SQLException {
            return "int8";
        }

        @Override
        public int getBaseType() throws SQLException {
            return Types.BIGINT;
        }

        @Override
        public Object getArray() throws SQLException {
            return elements;
        }

        @Override
        public Object getArray(Map<String, Class<?>> map) throws SQLException {
            return elements;
        }

        @Override
        public Object getArray(long index, int count) throws SQLException {
            return elements;
        }

        @Override
        public Object getArray(long index, int count, Map<String, Class<?>> map) throws SQLException {
            return elements;
        }

        @Override
        public ResultSet getResultSet() throws SQLException {
            return null;
        }

        @Override
        public ResultSet getResultSet(Map<String, Class<?>> map) throws SQLException {
            return null;
        }

        @Override
        public ResultSet getResultSet(long index, int count) throws SQLException {
            return null;
        }

        @Override
        public ResultSet getResultSet(long index, int count, Map<String, Class<?>> map) throws SQLException {
            return null;
        }

        @Override
        public void free() throws SQLException {
            // do nothing
        }
    }

    /**
     * Mock Array class with null elements for testing edge cases
     */
    private static class MockArrayWithNulls implements Array {
        private final Object[] elements = {1L, null, 3L};

        @Override
        public String getBaseTypeName() throws SQLException {
            return "int8";
        }

        @Override
        public int getBaseType() throws SQLException {
            return Types.BIGINT;
        }

        @Override
        public Object getArray() throws SQLException {
            return elements;
        }

        @Override
        public Object getArray(Map<String, Class<?>> map) throws SQLException {
            return elements;
        }

        @Override
        public Object getArray(long index, int count) throws SQLException {
            return elements;
        }

        @Override
        public Object getArray(long index, int count, Map<String, Class<?>> map) throws SQLException {
            return elements;
        }

        @Override
        public ResultSet getResultSet() throws SQLException {
            return null;
        }

        @Override
        public ResultSet getResultSet(Map<String, Class<?>> map) throws SQLException {
            return null;
        }

        @Override
        public ResultSet getResultSet(long index, int count) throws SQLException {
            return null;
        }

        @Override
        public ResultSet getResultSet(long index, int count, Map<String, Class<?>> map) throws SQLException {
            return null;
        }

        @Override
        public void free() throws SQLException {
            // do nothing
        }
    }
}
