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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.rm.datasource.DataCompareUtils;
import org.apache.seata.rm.datasource.sql.serial.SerialArray;
import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.undo.AbstractUndoLogManager;
import org.apache.seata.rm.datasource.undo.BaseUndoLogParserTest;
import org.apache.seata.rm.datasource.undo.UndoLogParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

import static org.mockito.Mockito.mockStatic;

public class JacksonUndoLogParserTest extends BaseUndoLogParserTest {

    JacksonUndoLogParser parser =
            (JacksonUndoLogParser) EnhancedServiceLoader.load(UndoLogParser.class, JacksonUndoLogParser.NAME);

    @Test
    public void encode() throws NoSuchFieldException, IllegalAccessException, IOException, SQLException {
        // get the jackson mapper
        java.lang.reflect.Field reflectField = parser.getClass().getDeclaredField("mapper");
        reflectField.setAccessible(true);
        ObjectMapper mapper = (ObjectMapper) reflectField.get(parser);

        // bigint type
        Field field = new Field("bigint_type", JDBCType.BIGINT.getVendorTypeNumber(), 9223372036854775807L);
        byte[] bytes = mapper.writeValueAsBytes(field);
        Field sameField = mapper.readValue(bytes, Field.class);
        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field, sameField).getResult());

        // big decimal type
        field = new Field(
                "decimal_type",
                JDBCType.DECIMAL.getVendorTypeNumber(),
                new BigDecimal("55555555555555555555.55555555555555555555"));
        bytes = mapper.writeValueAsBytes(field);
        sameField = mapper.readValue(bytes, Field.class);
        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field, sameField).getResult());

        // double type
        field = new Field("double_type", JDBCType.DOUBLE.getVendorTypeNumber(), 999999.999999999);
        bytes = mapper.writeValueAsBytes(field);
        sameField = mapper.readValue(bytes, Field.class);
        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field, sameField).getResult());

        // timestamp type: accurate to millisecond
        field = new Field(
                "timestamp_type",
                JDBCType.TIMESTAMP.getVendorTypeNumber(),
                Timestamp.valueOf("2021-05-18 17:23:22.111"));
        bytes = mapper.writeValueAsBytes(field);
        sameField = mapper.readValue(bytes, Field.class);
        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field, sameField).getResult());
        // timestamp type: accurate to microseconds
        field = new Field(
                "timestamp_type",
                JDBCType.TIMESTAMP.getVendorTypeNumber(),
                Timestamp.valueOf("2019-08-10 10:49:26.926554"));
        bytes = mapper.writeValueAsBytes(field);
        sameField = mapper.readValue(bytes, Field.class);
        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field, sameField).getResult());

        // blob type
        field = new Field("blob_type", JDBCType.BLOB.getVendorTypeNumber(), new SerialBlob("hello".getBytes()));
        bytes = mapper.writeValueAsBytes(field);
        sameField = mapper.readValue(bytes, Field.class);
        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field, sameField).getResult());

        // clob type
        field = new Field("clob_type", JDBCType.CLOB.getVendorTypeNumber(), new SerialClob("hello".toCharArray()));
        bytes = mapper.writeValueAsBytes(field);
        sameField = mapper.readValue(bytes, Field.class);
        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field, sameField).getResult());

        // LocalDateTime type: accurate to millisecond
        LocalDateTime dateTime = LocalDateTime.of(2021, 5, 18, 17, 23, 22, 111000000);
        field = new Field("localdatetime_type", JDBCType.DATE.getVendorTypeNumber(), dateTime);
        bytes = mapper.writeValueAsBytes(field);
        sameField = mapper.readValue(bytes, Field.class);
        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field, sameField).getResult());
        // LocalDateTime type: accurate to microseconds
        dateTime = LocalDateTime.of(2021, 5, 18, 17, 23, 22, 222333000);
        field = new Field("localdatetime_type2", JDBCType.DATE.getVendorTypeNumber(), dateTime);
        bytes = mapper.writeValueAsBytes(field);
        sameField = mapper.readValue(bytes, Field.class);
        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field, sameField).getResult());
    }

    @Test
    public void testSerializeAndDeserializeDmdbTimestamp()
            throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException,
                    InvocationTargetException, IOException {
        java.lang.reflect.Field reflectField = parser.getClass().getDeclaredField("mapper");
        reflectField.setAccessible(true);
        ObjectMapper mapper = (ObjectMapper) reflectField.get(parser);

        Class<?> dmdbTimestampClass = Class.forName("dm.jdbc.driver.DmdbTimestamp");
        Method valueOfMethod = dmdbTimestampClass.getMethod("valueOf", ZonedDateTime.class);
        Method valueOfDateMethod = dmdbTimestampClass.getMethod("valueOf", Date.class);

        Object dmdbTimestamp =
                valueOfMethod.invoke(null, Instant.ofEpochMilli(1721985847000L).atZone(ZoneId.systemDefault()));
        Field field = new Field("dmdb_timestamp", JDBCType.TIMESTAMP.getVendorTypeNumber(), dmdbTimestamp);
        byte[] bytes = mapper.writeValueAsBytes(field);
        Field sameField = mapper.readValue(bytes, Field.class);
        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field, sameField).getResult());

        Object originalTimestamp = valueOfDateMethod.invoke(null, new Date(1721985847000L));
        field = new Field("dmdb_timestamp_type2", JDBCType.TIMESTAMP.getVendorTypeNumber(), originalTimestamp);
        bytes = mapper.writeValueAsBytes(field);
        sameField = mapper.readValue(bytes, Field.class);
        Assertions.assertFalse(DataCompareUtils.isFieldEquals(field, sameField).getResult());

        Object dmdbTimestampnanos = valueOfMethod.invoke(
                null, Instant.ofEpochMilli(1721985847000L).plusNanos(12345L).atZone(ZoneId.systemDefault()));
        field = new Field("dmdb_timestamp_nanos", JDBCType.TIMESTAMP.getVendorTypeNumber(), dmdbTimestampnanos);
        bytes = mapper.writeValueAsBytes(field);
        sameField = mapper.readValue(bytes, Field.class);
        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field, sameField).getResult());
    }

    @Test
    public void testSerializeAndDeserializeDmdbTimestampWithNoZone()
            throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException,
                    InvocationTargetException, IOException {

        try (MockedStatic<AbstractUndoLogManager> mockedStatic = mockStatic(AbstractUndoLogManager.class)) {
            mockedStatic.when(AbstractUndoLogManager::getCurrentSerializer).thenReturn(JacksonUndoLogParser.NAME);

            java.lang.reflect.Field reflectField = parser.getClass().getDeclaredField("mapper");
            reflectField.setAccessible(true);
            ObjectMapper mapper = (ObjectMapper) reflectField.get(parser);

            Class<?> dmdbTimestampClass = Class.forName("dm.jdbc.driver.DmdbTimestamp");
            Method valueOfDateMethod = dmdbTimestampClass.getMethod("valueOf", Date.class);

            Object originalTimestamp = valueOfDateMethod.invoke(null, new Date(1721985847000L));
            Object originalTimestamp2 = valueOfDateMethod.invoke(null, new Date(1721985847001L));
            Field field = new Field("dmdb_timestamp_type", JDBCType.TIMESTAMP.getVendorTypeNumber(), originalTimestamp);
            Field field2 =
                    new Field("dmdb_timestamp_type", JDBCType.TIMESTAMP.getVendorTypeNumber(), originalTimestamp2);
            byte[] bytes = mapper.writeValueAsBytes(field);
            Field sameField = mapper.readValue(bytes, Field.class);
            Assertions.assertTrue(
                    DataCompareUtils.isFieldEquals(field, sameField).getResult());
            Assertions.assertFalse(DataCompareUtils.isFieldEquals(field, field2).getResult());
        }
    }

    @Test
    public void testSerializeAndDeserializeSerialArray()
            throws NoSuchFieldException, IllegalAccessException, IOException, SQLException {
        // get the jackson mapper
        java.lang.reflect.Field reflectField = parser.getClass().getDeclaredField("mapper");
        reflectField.setAccessible(true);
        ObjectMapper mapper = (ObjectMapper) reflectField.get(parser);

        // create a mock Array object for testing SerialArray
        Array mockArray = new MockArray();
        SerialArray serialArray = new SerialArray(mockArray);

        // test SerialArray with BIGINT array (PostgreSQL _int8 type)
        Field field = new Field("dept_ids", JDBCType.ARRAY.getVendorTypeNumber(), serialArray);
        byte[] bytes = mapper.writeValueAsBytes(field);
        Field sameField = mapper.readValue(bytes, Field.class);
        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field, sameField).getResult());

        // verify the SerialArray properties are correctly serialized/deserialized
        SerialArray deserializedArray = (SerialArray) sameField.getValue();
        Assertions.assertEquals(serialArray.getBaseType(), deserializedArray.getBaseType());
        Assertions.assertEquals(serialArray.getBaseTypeName(), deserializedArray.getBaseTypeName());
        Assertions.assertArrayEquals(serialArray.getElements(), deserializedArray.getElements());
    }

    @Test
    public void testSerializeAndDeserializeSerialArrayWithNulls()
            throws NoSuchFieldException, IllegalAccessException, IOException, SQLException {
        // get the jackson mapper
        java.lang.reflect.Field reflectField = parser.getClass().getDeclaredField("mapper");
        reflectField.setAccessible(true);
        ObjectMapper mapper = (ObjectMapper) reflectField.get(parser);

        // create SerialArray with null elements
        Array mockArrayWithNulls = new MockArrayWithNulls();
        SerialArray serialArray = new SerialArray(mockArrayWithNulls);

        Field field = new Field("nullable_array", JDBCType.ARRAY.getVendorTypeNumber(), serialArray);
        byte[] bytes = mapper.writeValueAsBytes(field);
        Field sameField = mapper.readValue(bytes, Field.class);

        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field, sameField).getResult());

        // verify null elements are handled correctly
        SerialArray deserializedArray = (SerialArray) sameField.getValue();
        Object[] elements = deserializedArray.getElements();
        Assertions.assertEquals(3, elements.length);
        Assertions.assertEquals(1L, elements[0]);
        Assertions.assertNull(elements[1]);
        Assertions.assertEquals(3L, elements[2]);
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

    @Override
    public UndoLogParser getParser() {
        return parser;
    }
}
