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
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterProvider;
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
import java.util.concurrent.atomic.AtomicInteger;

public class Fastjson2UndoLogParserTest extends BaseUndoLogParserTest {

    static class CustomValue {
        final String text;

        CustomValue(String text) {
            this.text = text;
        }
    }

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
    public void testWritersRegisteredOnTheDefaultProviderAreStillUsed() {
        AtomicInteger calls = new AtomicInteger();
        ObjectWriter<CustomValue> customWriter = (jsonWriter, object, fieldName, fieldType, features) -> {
            calls.incrementAndGet();
            jsonWriter.writeString("custom:" + ((CustomValue) object).text);
        };
        ObjectWriterProvider defaultProvider = JSONFactory.getDefaultObjectWriterProvider();
        defaultProvider.register(CustomValue.class, customWriter, true);
        try {
            Field field = new Field("payload", Types.OTHER, new CustomValue("a"));

            Field decodedField = getFieldFromLog(getParser().decode(getParser().encode(createTestUndoLog(field))));

            Assertions.assertEquals(1, calls.get());
            Assertions.assertEquals("custom:a", decodedField.getValue());
        } finally {
            defaultProvider.unregister(CustomValue.class, customWriter, true);
        }
    }

    @Test
    public void testWriterProviderUsesTheDefaultProviderForOtherTypes() {
        ObjectWriterProvider provider = new Fastjson2UndoLogParser.UndoLogWriterProvider();
        ObjectWriterProvider defaultProvider = JSONFactory.getDefaultObjectWriterProvider();

        Assertions.assertTrue(
                provider.getObjectWriter(Timestamp.class, Timestamp.class, true)
                        instanceof Fastjson2UndoLogParser.TimestampWriter);
        Assertions.assertTrue(
                provider.getObjectWriter(Timestamp.class, Timestamp.class, null, true)
                        instanceof Fastjson2UndoLogParser.TimestampWriter);
        Assertions.assertTrue(
                provider.getObjectWriterFromCache(Timestamp.class, Timestamp.class, true)
                        instanceof Fastjson2UndoLogParser.TimestampWriter);

        Assertions.assertSame(
                defaultProvider.getObjectWriter(String.class, String.class, true),
                provider.getObjectWriter(String.class, String.class, true));
        Assertions.assertSame(
                defaultProvider.getObjectWriter(String.class, String.class, null, true),
                provider.getObjectWriter(String.class, String.class, null, true));
        Assertions.assertSame(
                defaultProvider.getObjectWriterFromCache(String.class, String.class, true),
                provider.getObjectWriterFromCache(String.class, String.class, true));
    }

    @Test
    public void testWriterProviderKeepsItsOwnWritersForUndoLogClasses() {
        ObjectWriterProvider provider = new Fastjson2UndoLogParser.UndoLogWriterProvider();
        ObjectWriterProvider defaultProvider = JSONFactory.getDefaultObjectWriterProvider();

        for (Class<?> undoLogClass : new Class<?>[] {
            BranchUndoLog.class,
            SQLUndoLog.class,
            TableRecords.class,
            TableRecords.EmptyTableRecords.class,
            Row.class,
            Field.class
        }) {
            ObjectWriter<?> writer = provider.getObjectWriter(undoLogClass, undoLogClass, true);

            Assertions.assertNotSame(defaultProvider.getObjectWriter(undoLogClass, undoLogClass, true), writer);
            Assertions.assertSame(writer, provider.getObjectWriter(undoLogClass, undoLogClass, null, true));
            Assertions.assertSame(writer, provider.getObjectWriterFromCache(undoLogClass, undoLogClass, true));
        }
    }

    @Test
    public void testTimestampKeepsTypeAfterUndoLogClassesWereSerializedWithTheDefaultProvider() {
        Field field = new Field("gmt_modified", Types.TIMESTAMP, Timestamp.valueOf("2024-03-02 12:34:56.123456789"));
        cleanupDefaultProviderWriters();
        Fastjson2UndoLogParser freshParser = newParser();

        JSONB.toBytes(createTestUndoLog(field), UNDO_LOG_WRITER_FEATURES);
        Field decodedField = getFieldFromLog(freshParser.decode(freshParser.encode(createTestUndoLog(field))));

        Assertions.assertEquals(field.getValue(), decodedField.getValue());
    }

    @Test
    public void testTimestampWriterDoesNotLeakIntoTheDefaultProviderAfterEncodingAnUndoLog() {
        Timestamp timestamp = Timestamp.valueOf("2024-03-02 12:34:56.123456789");
        Field field = new Field("gmt_modified", Types.TIMESTAMP, timestamp);
        cleanupDefaultProviderWriters();
        Fastjson2UndoLogParser freshParser = newParser();

        Field decodedField = getFieldFromLog(freshParser.decode(freshParser.encode(createTestUndoLog(field))));
        byte[] defaultBytes = JSONB.toBytes(createTestUndoLog(field), UNDO_LOG_WRITER_FEATURES);

        Assertions.assertEquals(timestamp, decodedField.getValue());
        // the default provider still writes a Timestamp without its class name
        Assertions.assertEquals(
                timestamp.toLocalDateTime(),
                getFieldFromLog(freshParser.decode(defaultBytes)).getValue());
    }

    @Test
    public void testTimestampWriterWritesLocalDateTimeAsJsonText() {
        Timestamp timestamp = Timestamp.valueOf("2024-03-02 12:34:56.123456789");
        try (JSONWriter jsonWriter = JSONWriter.of()) {
            new Fastjson2UndoLogParser.TimestampWriter().write(jsonWriter, timestamp, null, null, 0L);
            Assertions.assertEquals("\"2024-03-02 12:34:56.123456789\"", jsonWriter.toString());
        }
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

    private static final JSONWriter.Feature[] UNDO_LOG_WRITER_FEATURES = {
        JSONWriter.Feature.WriteClassName,
        JSONWriter.Feature.FieldBased,
        JSONWriter.Feature.ReferenceDetection,
        JSONWriter.Feature.WriteNulls,
        JSONWriter.Feature.NotWriteDefaultValue,
        JSONWriter.Feature.NotWriteHashMapArrayListClassName,
        JSONWriter.Feature.WriteNameAsSymbol
    };

    private static Fastjson2UndoLogParser newParser() {
        Fastjson2UndoLogParser newParser = new Fastjson2UndoLogParser();
        newParser.init();
        return newParser;
    }

    /**
     * Drops the writers the default provider generated for the undo log classes, so a test starts with
     * field writers that have not cached a value writer yet.
     */
    private static void cleanupDefaultProviderWriters() {
        ObjectWriterProvider defaultProvider = JSONFactory.getDefaultObjectWriterProvider();
        defaultProvider.cleanup(BranchUndoLog.class);
        defaultProvider.cleanup(SQLUndoLog.class);
        defaultProvider.cleanup(TableRecords.class);
        defaultProvider.cleanup(TableRecords.EmptyTableRecords.class);
        defaultProvider.cleanup(Row.class);
        defaultProvider.cleanup(Field.class);
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
