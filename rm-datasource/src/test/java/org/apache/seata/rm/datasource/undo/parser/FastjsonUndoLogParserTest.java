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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.ValueFilter;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.rm.datasource.DataCompareUtils;
import org.apache.seata.rm.datasource.sql.serial.SerialArray;
import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.KeyType;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.BaseUndoLogParserTest;
import org.apache.seata.rm.datasource.undo.BranchUndoLog;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.rm.datasource.undo.UndoLogParser;
import org.apache.seata.sqlparser.SQLType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FastjsonUndoLogParserTest extends BaseUndoLogParserTest {

    FastjsonUndoLogParser parser =
            (FastjsonUndoLogParser) EnhancedServiceLoader.load(UndoLogParser.class, FastjsonUndoLogParser.NAME);

    @BeforeEach
    public void setUp() {
        // Ensure init() is called to register SerialArray serializers
        parser.init();
    }

    @Override
    public UndoLogParser getParser() {
        return parser;
    }

    /**
     * disable super testTimestampEncodeAndDecode
     */
    @Override
    public void testTimestampEncodeAndDecode() {
        Timestamp encodeStamp = new Timestamp(System.currentTimeMillis());
        encodeStamp.setNanos(999999);
        SerializeConfig.getGlobalInstance().addFilter(Timestamp.class, new TimestampSerializer());
        byte[] encode = JSON.toJSONString(encodeStamp, SerializeConfig.getGlobalInstance())
                .getBytes();
    }

    @Test
    public void testWriteClassName() throws Exception {
        TableRecords beforeImage = new TableRecords();
        TableRecords afterImage = new TableRecords();
        afterImage.setTableName("t1");
        List<Row> rows = new ArrayList<>();
        Row row = new Row();
        Field field = new Field();
        field.setName("id");
        field.setKeyType(KeyType.PRIMARY_KEY);
        field.setType(Types.BIGINT);
        field.setValue(Long.valueOf("0"));
        row.add(field);
        field = new Field();
        field.setName("money");
        field.setType(Types.DECIMAL);
        field.setValue(BigDecimal.ONE);
        row.add(field);
        rows.add(row);
        afterImage.setRows(rows);

        SQLUndoLog sqlUndoLog00 = new SQLUndoLog();
        sqlUndoLog00.setSqlType(SQLType.INSERT);
        sqlUndoLog00.setTableName("table_name");
        sqlUndoLog00.setBeforeImage(beforeImage);
        sqlUndoLog00.setAfterImage(afterImage);

        BranchUndoLog originLog = new BranchUndoLog();
        originLog.setBranchId(123456L);
        originLog.setXid("xiddddddddddd");
        List<SQLUndoLog> logList = new ArrayList<>();
        logList.add(sqlUndoLog00);
        originLog.setSqlUndoLogs(logList);

        // start test
        byte[] bs = getParser().encode(originLog);

        String s = new String(bs);
        Assertions.assertTrue(s.contains("\"@type\""));

        BranchUndoLog decode = getParser().decode(s.getBytes());
        Object value1 = decode.getSqlUndoLogs()
                .get(0)
                .getAfterImage()
                .getRows()
                .get(0)
                .getFields()
                .get(0)
                .getValue();
        Object value2 = decode.getSqlUndoLogs()
                .get(0)
                .getAfterImage()
                .getRows()
                .get(0)
                .getFields()
                .get(1)
                .getValue();
        Assertions.assertTrue(value1 instanceof Long);
        Assertions.assertTrue(value2 instanceof BigDecimal);
    }

    @Test
    public void testDirectSerialArraySerialization() throws SQLException {
        // Test direct SerialArray serialization without UndoLog wrapper
        Array mockArray = new MockArray();
        SerialArray serialArray = new SerialArray(mockArray);

        // Test with our custom serializers
        String json = JSON.toJSONString(serialArray, parser.serializeConfig);
        System.out.println("Direct SerialArray JSON: " + json);

        // Do not specify a specific type, let Fastjson automatically determine it based on the @type information
        SerialArray deserialized = (SerialArray) JSON.parse(json, parser.parserConfig);

        System.out.println("Original - baseType: " + serialArray.getBaseType() + ", baseTypeName: "
                + serialArray.getBaseTypeName() + ", elements: "
                + java.util.Arrays.toString(serialArray.getElements()));
        System.out.println("Deserialized - baseType: " + deserialized.getBaseType() + ", baseTypeName: "
                + deserialized.getBaseTypeName() + ", elements: "
                + java.util.Arrays.toString(deserialized.getElements()));

        Assertions.assertEquals(serialArray.getBaseType(), deserialized.getBaseType());
        Assertions.assertEquals(serialArray.getBaseTypeName(), deserialized.getBaseTypeName());
        Assertions.assertArrayEquals(serialArray.getElements(), deserialized.getElements());
    }

    @Test
    public void testSerializeAndDeserializeSerialArray() throws IOException, SQLException {
        // create a mock Array object for testing SerialArray
        Array mockArray = new MockArray();
        SerialArray serialArray = new SerialArray(mockArray);

        // test SerialArray with BIGINT array (PostgreSQL _int8 type)
        Field field = new Field("dept_ids", JDBCType.ARRAY.getVendorTypeNumber(), serialArray);

        // Debug: Print JSON to see if our serializer is being used
        byte[] bytes = getParser().encode(createTestUndoLog(field));
        String jsonString = new String(bytes);
        System.out.println("Serialized JSON: " + jsonString);

        BranchUndoLog decodedLog = getParser().decode(bytes);
        Field decodedField = getFieldFromLog(decodedLog);

        // Debug information: print original and deserialized SerialArray properties
        SerialArray original = (SerialArray) field.getValue();
        SerialArray deserialized = (SerialArray) decodedField.getValue();

        System.out.println("Original - baseType: " + original.getBaseType() + ", baseTypeName: "
                + original.getBaseTypeName() + ", elements: "
                + java.util.Arrays.toString(original.getElements()));
        System.out.println("Deserialized - baseType: " + deserialized.getBaseType() + ", baseTypeName: "
                + deserialized.getBaseTypeName() + ", elements: "
                + java.util.Arrays.toString(deserialized.getElements()));

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
        TableRecords afterImage = new TableRecords();
        afterImage.setTableName("test_table");
        List<Row> rows = new ArrayList<>();
        Row row = new Row();
        row.add(field);
        rows.add(row);
        afterImage.setRows(rows);

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.INSERT);
        sqlUndoLog.setTableName("test_table");
        sqlUndoLog.setBeforeImage(new TableRecords());
        sqlUndoLog.setAfterImage(afterImage);

        BranchUndoLog branchUndoLog = new BranchUndoLog();
        branchUndoLog.setBranchId(123456L);
        branchUndoLog.setXid("test_xid");
        List<SQLUndoLog> logList = new ArrayList<>();
        logList.add(sqlUndoLog);
        branchUndoLog.setSqlUndoLogs(logList);

        return branchUndoLog;
    }

    private Field getFieldFromLog(BranchUndoLog log) {
        return log.getSqlUndoLogs()
                .get(0)
                .getAfterImage()
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

    private class TimestampSerializer implements ValueFilter {

        @Override
        public Object process(Object object, String name, Object value) {
            return null;
        }
    }
}
