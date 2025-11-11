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
package org.apache.seata.rm.datasource.xa;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * Tests for PreparedStatementProxyXA
 *
 */
public class PreparedStatementProxyXATest {

    private AbstractConnectionProxyXA mockConnectionProxyXA;
    private PreparedStatement mockPreparedStatement;
    private PreparedStatementProxyXA preparedStatementProxyXA;

    @BeforeEach
    public void setUp() {
        mockConnectionProxyXA = Mockito.mock(AbstractConnectionProxyXA.class);
        mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        preparedStatementProxyXA = new PreparedStatementProxyXA(mockConnectionProxyXA, mockPreparedStatement);
    }

    @Test
    public void testExecuteQuery() throws SQLException {
        // Mock ExecuteTemplateXA.execute to return a ResultSet
        ResultSet mockResultSet = Mockito.mock(ResultSet.class);

        // Create a test implementation that we can control
        PreparedStatementProxyXA testProxy =
                new PreparedStatementProxyXA(mockConnectionProxyXA, mockPreparedStatement) {
                    @Override
                    public ResultSet executeQuery() throws SQLException {
                        // Directly call the target statement for testing
                        return mockPreparedStatement.executeQuery();
                    }
                };

        Mockito.when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        // Execute
        ResultSet result = testProxy.executeQuery();

        // Verify
        Assertions.assertEquals(mockResultSet, result);
        Mockito.verify(mockPreparedStatement).executeQuery();
    }

    @Test
    public void testExecuteUpdate() throws SQLException {
        // Create a test implementation that we can control
        PreparedStatementProxyXA testProxy =
                new PreparedStatementProxyXA(mockConnectionProxyXA, mockPreparedStatement) {
                    @Override
                    public int executeUpdate() throws SQLException {
                        // Directly call the target statement for testing
                        return mockPreparedStatement.executeUpdate();
                    }
                };

        Mockito.when(mockPreparedStatement.executeUpdate()).thenReturn(5);

        // Execute
        int result = testProxy.executeUpdate();

        // Verify
        Assertions.assertEquals(5, result);
        Mockito.verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    public void testExecute() throws SQLException {
        // Create a test implementation that we can control
        PreparedStatementProxyXA testProxy =
                new PreparedStatementProxyXA(mockConnectionProxyXA, mockPreparedStatement) {
                    @Override
                    public boolean execute() throws SQLException {
                        // Directly call the target statement for testing
                        return mockPreparedStatement.execute();
                    }
                };

        Mockito.when(mockPreparedStatement.execute()).thenReturn(true);

        // Execute
        boolean result = testProxy.execute();

        // Verify
        Assertions.assertTrue(result);
        Mockito.verify(mockPreparedStatement).execute();
    }

    @Test
    public void testSetNull() throws SQLException {
        preparedStatementProxyXA.setNull(1, java.sql.Types.VARCHAR);

        Mockito.verify(mockPreparedStatement).setNull(1, java.sql.Types.VARCHAR);
    }

    @Test
    public void testSetBoolean() throws SQLException {
        preparedStatementProxyXA.setBoolean(1, true);

        Mockito.verify(mockPreparedStatement).setBoolean(1, true);
    }

    @Test
    public void testSetByte() throws SQLException {
        preparedStatementProxyXA.setByte(1, (byte) 123);

        Mockito.verify(mockPreparedStatement).setByte(1, (byte) 123);
    }

    @Test
    public void testSetShort() throws SQLException {
        preparedStatementProxyXA.setShort(1, (short) 12345);

        Mockito.verify(mockPreparedStatement).setShort(1, (short) 12345);
    }

    @Test
    public void testSetInt() throws SQLException {
        preparedStatementProxyXA.setInt(1, 123456);

        Mockito.verify(mockPreparedStatement).setInt(1, 123456);
    }

    @Test
    public void testSetLong() throws SQLException {
        preparedStatementProxyXA.setLong(1, 123456789L);

        Mockito.verify(mockPreparedStatement).setLong(1, 123456789L);
    }

    @Test
    public void testSetFloat() throws SQLException {
        preparedStatementProxyXA.setFloat(1, 123.45f);

        Mockito.verify(mockPreparedStatement).setFloat(1, 123.45f);
    }

    @Test
    public void testSetDouble() throws SQLException {
        preparedStatementProxyXA.setDouble(1, 123.456);

        Mockito.verify(mockPreparedStatement).setDouble(1, 123.456);
    }

    @Test
    public void testSetBigDecimal() throws SQLException {
        BigDecimal bigDecimal = new BigDecimal("123.456");
        preparedStatementProxyXA.setBigDecimal(1, bigDecimal);

        Mockito.verify(mockPreparedStatement).setBigDecimal(1, bigDecimal);
    }

    @Test
    public void testSetString() throws SQLException {
        preparedStatementProxyXA.setString(1, "test string");

        Mockito.verify(mockPreparedStatement).setString(1, "test string");
    }

    @Test
    public void testSetBytes() throws SQLException {
        byte[] bytes = {1, 2, 3, 4, 5};
        preparedStatementProxyXA.setBytes(1, bytes);

        Mockito.verify(mockPreparedStatement).setBytes(1, bytes);
    }

    @Test
    public void testSetDate() throws SQLException {
        Date date = new Date(System.currentTimeMillis());
        preparedStatementProxyXA.setDate(1, date);

        Mockito.verify(mockPreparedStatement).setDate(1, date);
    }

    @Test
    public void testSetTime() throws SQLException {
        Time time = new Time(System.currentTimeMillis());
        preparedStatementProxyXA.setTime(1, time);

        Mockito.verify(mockPreparedStatement).setTime(1, time);
    }

    @Test
    public void testSetTimestamp() throws SQLException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        preparedStatementProxyXA.setTimestamp(1, timestamp);

        Mockito.verify(mockPreparedStatement).setTimestamp(1, timestamp);
    }

    @Test
    public void testSetAsciiStream() throws SQLException {
        InputStream inputStream = Mockito.mock(InputStream.class);
        preparedStatementProxyXA.setAsciiStream(1, inputStream, 100);

        Mockito.verify(mockPreparedStatement).setAsciiStream(1, inputStream);
    }

    @Test
    public void testSetUnicodeStream() throws SQLException {
        InputStream inputStream = Mockito.mock(InputStream.class);
        preparedStatementProxyXA.setUnicodeStream(1, inputStream, 100);

        Mockito.verify(mockPreparedStatement).setUnicodeStream(1, inputStream, 100);
    }

    @Test
    public void testSetBinaryStream() throws SQLException {
        InputStream inputStream = Mockito.mock(InputStream.class);
        preparedStatementProxyXA.setBinaryStream(1, inputStream, 100);

        Mockito.verify(mockPreparedStatement).setBinaryStream(1, inputStream, 100);
    }

    @Test
    public void testClearParameters() throws SQLException {
        preparedStatementProxyXA.clearParameters();

        Mockito.verify(mockPreparedStatement).clearParameters();
    }

    @Test
    public void testSetObject() throws SQLException {
        Object obj = new Object();
        preparedStatementProxyXA.setObject(1, obj, java.sql.Types.VARCHAR);

        Mockito.verify(mockPreparedStatement).setObject(1, obj, java.sql.Types.VARCHAR);
    }

    @Test
    public void testSetObjectWithoutSqlType() throws SQLException {
        Object obj = "test";
        preparedStatementProxyXA.setObject(1, obj);

        Mockito.verify(mockPreparedStatement).setObject(1, obj);
    }

    @Test
    public void testAddBatch() throws SQLException {
        preparedStatementProxyXA.addBatch();

        Mockito.verify(mockPreparedStatement).addBatch();
    }

    @Test
    public void testSetCharacterStream() throws SQLException {
        Reader reader = Mockito.mock(Reader.class);
        preparedStatementProxyXA.setCharacterStream(1, reader, 100);

        Mockito.verify(mockPreparedStatement).setCharacterStream(1, reader, 100);
    }

    @Test
    public void testSetRef() throws SQLException {
        java.sql.Ref ref = Mockito.mock(java.sql.Ref.class);
        preparedStatementProxyXA.setRef(1, ref);

        Mockito.verify(mockPreparedStatement).setRef(1, ref);
    }

    @Test
    public void testSetBlob() throws SQLException {
        Blob blob = Mockito.mock(Blob.class);
        preparedStatementProxyXA.setBlob(1, blob);

        Mockito.verify(mockPreparedStatement).setBlob(1, blob);
    }

    @Test
    public void testSetClob() throws SQLException {
        Clob clob = Mockito.mock(Clob.class);
        preparedStatementProxyXA.setClob(1, clob);

        Mockito.verify(mockPreparedStatement).setClob(1, clob);
    }

    @Test
    public void testSetArray() throws SQLException {
        Array array = Mockito.mock(Array.class);
        preparedStatementProxyXA.setArray(1, array);

        Mockito.verify(mockPreparedStatement).setArray(1, array);
    }

    @Test
    public void testGetMetaData() throws SQLException {
        ResultSetMetaData mockMetaData = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(mockPreparedStatement.getMetaData()).thenReturn(mockMetaData);

        ResultSetMetaData result = preparedStatementProxyXA.getMetaData();

        Assertions.assertEquals(mockMetaData, result);
        Mockito.verify(mockPreparedStatement).getMetaData();
    }

    @Test
    public void testSetDateWithCalendar() throws SQLException {
        Date date = new Date(System.currentTimeMillis());
        Calendar calendar = Calendar.getInstance();
        preparedStatementProxyXA.setDate(1, date, calendar);

        Mockito.verify(mockPreparedStatement).setDate(1, date, calendar);
    }

    @Test
    public void testSetTimeWithCalendar() throws SQLException {
        Time time = new Time(System.currentTimeMillis());
        Calendar calendar = Calendar.getInstance();
        preparedStatementProxyXA.setTime(1, time, calendar);

        Mockito.verify(mockPreparedStatement).setTime(1, time, calendar);
    }

    @Test
    public void testSetTimestampWithCalendar() throws SQLException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Calendar calendar = Calendar.getInstance();
        preparedStatementProxyXA.setTimestamp(1, timestamp, calendar);

        Mockito.verify(mockPreparedStatement).setTimestamp(1, timestamp, calendar);
    }

    @Test
    public void testSetNullWithTypeName() throws SQLException {
        preparedStatementProxyXA.setNull(1, java.sql.Types.VARCHAR, "VARCHAR");

        Mockito.verify(mockPreparedStatement).setNull(1, java.sql.Types.VARCHAR, "VARCHAR");
    }

    @Test
    public void testSetURL() throws SQLException {
        URL url = Mockito.mock(URL.class);
        preparedStatementProxyXA.setURL(1, url);

        Mockito.verify(mockPreparedStatement).setURL(1, url);
    }

    @Test
    public void testGetParameterMetaData() throws SQLException {
        ParameterMetaData mockParamMetaData = Mockito.mock(ParameterMetaData.class);
        Mockito.when(mockPreparedStatement.getParameterMetaData()).thenReturn(mockParamMetaData);

        ParameterMetaData result = preparedStatementProxyXA.getParameterMetaData();

        Assertions.assertEquals(mockParamMetaData, result);
        Mockito.verify(mockPreparedStatement).getParameterMetaData();
    }

    @Test
    public void testSetRowId() throws SQLException {
        RowId rowId = Mockito.mock(RowId.class);
        preparedStatementProxyXA.setRowId(1, rowId);

        Mockito.verify(mockPreparedStatement).setRowId(1, rowId);
    }

    @Test
    public void testSetNString() throws SQLException {
        preparedStatementProxyXA.setNString(1, "test nstring");

        Mockito.verify(mockPreparedStatement).setNString(1, "test nstring");
    }

    @Test
    public void testSetNCharacterStream() throws SQLException {
        Reader reader = Mockito.mock(Reader.class);
        preparedStatementProxyXA.setNCharacterStream(1, reader, 100L);

        Mockito.verify(mockPreparedStatement).setNCharacterStream(1, reader, 100L);
    }

    @Test
    public void testSetNClob() throws SQLException {
        NClob nClob = Mockito.mock(NClob.class);
        preparedStatementProxyXA.setNClob(1, nClob);

        Mockito.verify(mockPreparedStatement).setNClob(1, nClob);
    }

    @Test
    public void testSetClobWithReaderAndLength() throws SQLException {
        Reader reader = Mockito.mock(Reader.class);
        preparedStatementProxyXA.setClob(1, reader, 100L);

        Mockito.verify(mockPreparedStatement).setClob(1, reader, 100L);
    }

    @Test
    public void testSetBlobWithInputStreamAndLength() throws SQLException {
        InputStream inputStream = Mockito.mock(InputStream.class);
        preparedStatementProxyXA.setBlob(1, inputStream, 100L);

        Mockito.verify(mockPreparedStatement).setBlob(1, inputStream, 100L);
    }

    @Test
    public void testSetNClobWithReaderAndLength() throws SQLException {
        Reader reader = Mockito.mock(Reader.class);
        preparedStatementProxyXA.setNClob(1, reader, 100L);

        Mockito.verify(mockPreparedStatement).setNClob(1, reader, 100L);
    }

    @Test
    public void testSetSQLXML() throws SQLException {
        SQLXML sqlXml = Mockito.mock(SQLXML.class);
        preparedStatementProxyXA.setSQLXML(1, sqlXml);

        Mockito.verify(mockPreparedStatement).setSQLXML(1, sqlXml);
    }

    @Test
    public void testSetObjectWithScale() throws SQLException {
        BigDecimal bigDecimal = new BigDecimal("123.456");
        preparedStatementProxyXA.setObject(1, bigDecimal, java.sql.Types.DECIMAL, 2);

        Mockito.verify(mockPreparedStatement).setObject(1, bigDecimal, java.sql.Types.DECIMAL, 2);
    }

    @Test
    public void testSetAsciiStreamLong() throws SQLException {
        InputStream inputStream = Mockito.mock(InputStream.class);
        preparedStatementProxyXA.setAsciiStream(1, inputStream, 100L);

        Mockito.verify(mockPreparedStatement).setAsciiStream(1, inputStream, 100L);
    }

    @Test
    public void testSetBinaryStreamLong() throws SQLException {
        InputStream inputStream = Mockito.mock(InputStream.class);
        preparedStatementProxyXA.setBinaryStream(1, inputStream, 100L);

        Mockito.verify(mockPreparedStatement).setBinaryStream(1, inputStream, 100L);
    }

    @Test
    public void testSetCharacterStreamLong() throws SQLException {
        Reader reader = Mockito.mock(Reader.class);
        preparedStatementProxyXA.setCharacterStream(1, reader, 100L);

        Mockito.verify(mockPreparedStatement).setCharacterStream(1, reader, 100L);
    }

    @Test
    public void testSetAsciiStreamWithoutLength() throws SQLException {
        InputStream inputStream = Mockito.mock(InputStream.class);
        preparedStatementProxyXA.setAsciiStream(1, inputStream);

        Mockito.verify(mockPreparedStatement).setAsciiStream(1, inputStream);
    }

    @Test
    public void testSetBinaryStreamWithoutLength() throws SQLException {
        InputStream inputStream = Mockito.mock(InputStream.class);
        preparedStatementProxyXA.setBinaryStream(1, inputStream);

        Mockito.verify(mockPreparedStatement).setBinaryStream(1, inputStream);
    }

    @Test
    public void testSetCharacterStreamWithoutLength() throws SQLException {
        Reader reader = Mockito.mock(Reader.class);
        preparedStatementProxyXA.setCharacterStream(1, reader);

        Mockito.verify(mockPreparedStatement).setCharacterStream(1, reader);
    }

    @Test
    public void testSetNCharacterStreamWithoutLength() throws SQLException {
        Reader reader = Mockito.mock(Reader.class);
        preparedStatementProxyXA.setNCharacterStream(1, reader);

        Mockito.verify(mockPreparedStatement).setNCharacterStream(1, reader);
    }

    @Test
    public void testSetClobWithReader() throws SQLException {
        Reader reader = Mockito.mock(Reader.class);
        preparedStatementProxyXA.setClob(1, reader);

        Mockito.verify(mockPreparedStatement).setClob(1, reader);
    }

    @Test
    public void testSetBlobWithInputStream() throws SQLException {
        InputStream inputStream = Mockito.mock(InputStream.class);
        preparedStatementProxyXA.setBlob(1, inputStream);

        Mockito.verify(mockPreparedStatement).setBlob(1, inputStream);
    }

    @Test
    public void testSetNClobWithReader() throws SQLException {
        Reader reader = Mockito.mock(Reader.class);
        preparedStatementProxyXA.setNClob(1, reader);

        Mockito.verify(mockPreparedStatement).setNClob(1, reader);
    }
}
