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
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;

/**
 * Tests for PreparedStatementProxyXA
 * Focus on verifying actual results and business logic, not just method calls
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
    public void testExecuteQueryReturnsCorrectResultSet() throws SQLException {
        // Verify executeQuery returns the correct ResultSet through XA transaction flow
        ResultSet expectedResultSet = Mockito.mock(ResultSet.class);

        try (MockedStatic<ExecuteTemplateXA> mockedExecuteTemplate = Mockito.mockStatic(ExecuteTemplateXA.class)) {
            mockedExecuteTemplate
                    .when(() -> ExecuteTemplateXA.execute(
                            Mockito.eq(mockConnectionProxyXA), Mockito.any(), Mockito.eq(mockPreparedStatement)))
                    .thenReturn(expectedResultSet);

            ResultSet actualResultSet = preparedStatementProxyXA.executeQuery();

            Assertions.assertSame(
                    expectedResultSet,
                    actualResultSet,
                    "executeQuery should return the exact ResultSet from ExecuteTemplateXA");
        }
    }

    @Test
    public void testExecuteUpdateReturnsCorrectCount() throws SQLException {
        // Verify executeUpdate returns the correct update count through XA transaction flow
        int expectedUpdateCount = 42;

        try (MockedStatic<ExecuteTemplateXA> mockedExecuteTemplate = Mockito.mockStatic(ExecuteTemplateXA.class)) {
            mockedExecuteTemplate
                    .when(() -> ExecuteTemplateXA.execute(
                            Mockito.eq(mockConnectionProxyXA), Mockito.any(), Mockito.eq(mockPreparedStatement)))
                    .thenReturn(expectedUpdateCount);

            int actualUpdateCount = preparedStatementProxyXA.executeUpdate();

            Assertions.assertEquals(
                    expectedUpdateCount,
                    actualUpdateCount,
                    "executeUpdate should return the exact count from ExecuteTemplateXA");
        }
    }

    @Test
    public void testExecuteReturnsCorrectBoolean() throws SQLException {
        // Verify execute returns the correct boolean result through XA transaction flow
        boolean expectedResult = true;

        try (MockedStatic<ExecuteTemplateXA> mockedExecuteTemplate = Mockito.mockStatic(ExecuteTemplateXA.class)) {
            mockedExecuteTemplate
                    .when(() -> ExecuteTemplateXA.execute(
                            Mockito.eq(mockConnectionProxyXA), Mockito.any(), Mockito.eq(mockPreparedStatement)))
                    .thenReturn(expectedResult);

            boolean actualResult = preparedStatementProxyXA.execute();

            Assertions.assertEquals(
                    expectedResult, actualResult, "execute should return the exact boolean from ExecuteTemplateXA");
        }
    }

    @Test
    public void testExecuteQueryPropagatesException() {
        // Verify exceptions are correctly propagated from XA transaction context
        SQLException expectedException = new SQLException("Query execution failed");

        try (MockedStatic<ExecuteTemplateXA> mockedExecuteTemplate = Mockito.mockStatic(ExecuteTemplateXA.class)) {
            mockedExecuteTemplate
                    .when(() -> ExecuteTemplateXA.execute(
                            Mockito.eq(mockConnectionProxyXA), Mockito.any(), Mockito.eq(mockPreparedStatement)))
                    .thenThrow(expectedException);

            SQLException actualException = Assertions.assertThrows(SQLException.class, () -> {
                preparedStatementProxyXA.executeQuery();
            });

            Assertions.assertSame(
                    expectedException, actualException, "Exception should be propagated without modification");
        }
    }

    @Test
    public void testGetMetaDataReturnsCorrectMetadata() throws SQLException {
        // Verify getMetaData returns the correct metadata object
        ResultSetMetaData expectedMetaData = Mockito.mock(ResultSetMetaData.class);

        Mockito.when(mockPreparedStatement.getMetaData()).thenReturn(expectedMetaData);

        ResultSetMetaData actualMetaData = preparedStatementProxyXA.getMetaData();

        Assertions.assertSame(
                expectedMetaData,
                actualMetaData,
                "getMetaData should return the exact metadata from underlying statement");
    }

    @Test
    public void testGetParameterMetaDataReturnsCorrectMetadata() throws SQLException {
        // Verify getParameterMetaData returns the correct parameter metadata object
        ParameterMetaData expectedParamMetaData = Mockito.mock(ParameterMetaData.class);

        Mockito.when(mockPreparedStatement.getParameterMetaData()).thenReturn(expectedParamMetaData);

        ParameterMetaData actualParamMetaData = preparedStatementProxyXA.getParameterMetaData();

        Assertions.assertSame(
                expectedParamMetaData,
                actualParamMetaData,
                "getParameterMetaData should return the exact parameter metadata from underlying statement");
    }

    @Test
    public void testParameterSettingDoesNotBreakExecution() throws SQLException {
        // Verify that setting various parameters doesn't break query execution
        BigDecimal testDecimal = new BigDecimal("123.45");
        Date testDate = new Date(System.currentTimeMillis());
        ResultSet expectedResultSet = Mockito.mock(ResultSet.class);

        // Set various parameters
        preparedStatementProxyXA.setString(1, "testString");
        preparedStatementProxyXA.setInt(2, 42);
        preparedStatementProxyXA.setLong(3, 123456789L);
        preparedStatementProxyXA.setBigDecimal(4, testDecimal);
        preparedStatementProxyXA.setDate(5, testDate);
        preparedStatementProxyXA.setNull(6, Types.VARCHAR);

        // Execute query and verify it returns the expected result
        try (MockedStatic<ExecuteTemplateXA> mockedExecuteTemplate = Mockito.mockStatic(ExecuteTemplateXA.class)) {
            mockedExecuteTemplate
                    .when(() -> ExecuteTemplateXA.execute(
                            Mockito.eq(mockConnectionProxyXA), Mockito.any(), Mockito.eq(mockPreparedStatement)))
                    .thenReturn(expectedResultSet);

            ResultSet actualResultSet = preparedStatementProxyXA.executeQuery();

            Assertions.assertSame(
                    expectedResultSet,
                    actualResultSet,
                    "Query should execute successfully and return expected ResultSet after setting parameters");
        }
    }

    @Test
    public void testStreamParametersDoNotBreakExecution() throws SQLException {
        // Verify that setting stream parameters doesn't break query execution
        InputStream mockInputStream = Mockito.mock(InputStream.class);
        Reader mockReader = Mockito.mock(Reader.class);
        Blob mockBlob = Mockito.mock(Blob.class);
        int expectedUpdateCount = 7;

        // Set stream parameters
        preparedStatementProxyXA.setAsciiStream(1, mockInputStream, 100);
        preparedStatementProxyXA.setBinaryStream(2, mockInputStream, 200);
        preparedStatementProxyXA.setCharacterStream(3, mockReader, 300);
        preparedStatementProxyXA.setBlob(4, mockBlob);

        // Execute update and verify it returns the expected count
        try (MockedStatic<ExecuteTemplateXA> mockedExecuteTemplate = Mockito.mockStatic(ExecuteTemplateXA.class)) {
            mockedExecuteTemplate
                    .when(() -> ExecuteTemplateXA.execute(
                            Mockito.eq(mockConnectionProxyXA), Mockito.any(), Mockito.eq(mockPreparedStatement)))
                    .thenReturn(expectedUpdateCount);

            int actualUpdateCount = preparedStatementProxyXA.executeUpdate();

            Assertions.assertEquals(
                    expectedUpdateCount,
                    actualUpdateCount,
                    "Update should execute successfully and return expected count after setting stream parameters");
        }
    }

    @Test
    public void testComplexParametersDoNotBreakExecution() throws SQLException {
        // Verify that setting complex parameters (dates with calendar, objects) doesn't break execution
        Date testDate = new Date(System.currentTimeMillis());
        Time testTime = new Time(System.currentTimeMillis());
        Timestamp testTimestamp = new Timestamp(System.currentTimeMillis());
        Calendar testCalendar = Calendar.getInstance();
        BigDecimal testDecimal = new BigDecimal("999.99");
        boolean expectedResult = true;

        // Set complex parameters
        preparedStatementProxyXA.setDate(1, testDate, testCalendar);
        preparedStatementProxyXA.setTime(2, testTime, testCalendar);
        preparedStatementProxyXA.setTimestamp(3, testTimestamp, testCalendar);
        preparedStatementProxyXA.setObject(4, "testObject");
        preparedStatementProxyXA.setObject(5, testDecimal, Types.DECIMAL);
        preparedStatementProxyXA.setObject(6, testDecimal, Types.DECIMAL, 2);

        // Execute and verify it returns the expected result
        try (MockedStatic<ExecuteTemplateXA> mockedExecuteTemplate = Mockito.mockStatic(ExecuteTemplateXA.class)) {
            mockedExecuteTemplate
                    .when(() -> ExecuteTemplateXA.execute(
                            Mockito.eq(mockConnectionProxyXA), Mockito.any(), Mockito.eq(mockPreparedStatement)))
                    .thenReturn(expectedResult);

            boolean actualResult = preparedStatementProxyXA.execute();

            Assertions.assertEquals(
                    expectedResult,
                    actualResult,
                    "Execute should return expected result after setting complex parameters");
        }
    }

    @Test
    public void testInheritedGetterMethodsReturnCorrectValues() throws SQLException {
        // Verify that methods inherited from StatementProxyXA return correct values
        Connection expectedConnection = Mockito.mock(Connection.class);
        ResultSet expectedResultSet = Mockito.mock(ResultSet.class);
        SQLWarning expectedWarning = Mockito.mock(SQLWarning.class);

        Mockito.when(mockPreparedStatement.getConnection()).thenReturn(expectedConnection);
        Mockito.when(mockPreparedStatement.getResultSet()).thenReturn(expectedResultSet);
        Mockito.when(mockPreparedStatement.getUpdateCount()).thenReturn(100);
        Mockito.when(mockPreparedStatement.getWarnings()).thenReturn(expectedWarning);
        Mockito.when(mockPreparedStatement.getMaxRows()).thenReturn(1000);
        Mockito.when(mockPreparedStatement.getQueryTimeout()).thenReturn(30);
        Mockito.when(mockPreparedStatement.isClosed()).thenReturn(false);
        Mockito.when(mockPreparedStatement.getFetchSize()).thenReturn(50);
        Mockito.when(mockPreparedStatement.getResultSetConcurrency()).thenReturn(ResultSet.CONCUR_READ_ONLY);
        Mockito.when(mockPreparedStatement.getResultSetType()).thenReturn(ResultSet.TYPE_FORWARD_ONLY);

        // Verify all getter methods return correct values
        Assertions.assertSame(
                expectedConnection,
                preparedStatementProxyXA.getConnection(),
                "getConnection should return the correct connection");
        Assertions.assertSame(
                expectedResultSet,
                preparedStatementProxyXA.getResultSet(),
                "getResultSet should return the correct result set");
        Assertions.assertEquals(
                100, preparedStatementProxyXA.getUpdateCount(), "getUpdateCount should return the correct count");
        Assertions.assertSame(
                expectedWarning,
                preparedStatementProxyXA.getWarnings(),
                "getWarnings should return the correct warning");
        Assertions.assertEquals(
                1000, preparedStatementProxyXA.getMaxRows(), "getMaxRows should return the correct value");
        Assertions.assertEquals(
                30, preparedStatementProxyXA.getQueryTimeout(), "getQueryTimeout should return the correct timeout");
        Assertions.assertFalse(preparedStatementProxyXA.isClosed(), "isClosed should return the correct state");
        Assertions.assertEquals(
                50, preparedStatementProxyXA.getFetchSize(), "getFetchSize should return the correct size");
        Assertions.assertEquals(
                ResultSet.CONCUR_READ_ONLY,
                preparedStatementProxyXA.getResultSetConcurrency(),
                "getResultSetConcurrency should return the correct concurrency");
        Assertions.assertEquals(
                ResultSet.TYPE_FORWARD_ONLY,
                preparedStatementProxyXA.getResultSetType(),
                "getResultSetType should return the correct type");
    }

    @Test
    public void testConstructorAndGetTargetStatement() {
        // Verify constructor properly initializes and getTargetStatement returns correct object
        AbstractConnectionProxyXA testConnectionProxy = Mockito.mock(AbstractConnectionProxyXA.class);
        PreparedStatement testPreparedStatement = Mockito.mock(PreparedStatement.class);

        PreparedStatementProxyXA proxy = new PreparedStatementProxyXA(testConnectionProxy, testPreparedStatement);

        // The getTargetStatement method is private, but we can verify it works through other methods
        Assertions.assertNotNull(proxy, "Constructor should create a valid proxy instance");

        // Verify that the proxy uses the correct target statement by checking method delegation
        try {
            proxy.clearParameters();
            Mockito.verify(testPreparedStatement).clearParameters();
        } catch (SQLException e) {
            // This is fine for the test, we just want to verify delegation
        }
    }

    @Test
    public void testBasicParameterSetters() throws SQLException {
        // Test all basic parameter setter methods to improve coverage
        preparedStatementProxyXA.setBoolean(1, true);
        preparedStatementProxyXA.setByte(2, (byte) 127);
        preparedStatementProxyXA.setShort(3, (short) 32767);
        preparedStatementProxyXA.setFloat(4, 3.14f);
        preparedStatementProxyXA.setDouble(5, 2.718281828);
        preparedStatementProxyXA.setBytes(6, new byte[] {1, 2, 3, 4, 5});

        // Verify all calls were delegated to the underlying statement
        Mockito.verify(mockPreparedStatement).setBoolean(1, true);
        Mockito.verify(mockPreparedStatement).setByte(2, (byte) 127);
        Mockito.verify(mockPreparedStatement).setShort(3, (short) 32767);
        Mockito.verify(mockPreparedStatement).setFloat(4, 3.14f);
        Mockito.verify(mockPreparedStatement).setDouble(5, 2.718281828);
        Mockito.verify(mockPreparedStatement).setBytes(6, new byte[] {1, 2, 3, 4, 5});
    }

    @Test
    public void testTimeRelatedParameterSetters() throws SQLException {
        // Test time-related parameter setters
        Time testTime = new Time(System.currentTimeMillis());
        Timestamp testTimestamp = new Timestamp(System.currentTimeMillis());

        preparedStatementProxyXA.setTime(1, testTime);
        preparedStatementProxyXA.setTimestamp(2, testTimestamp);

        // Verify calls were delegated
        Mockito.verify(mockPreparedStatement).setTime(1, testTime);
        Mockito.verify(mockPreparedStatement).setTimestamp(2, testTimestamp);
    }

    @Test
    public void testLargeObjectAndSpecialParameterSetters() throws SQLException {
        // Test setters for large objects and special parameters
        Ref mockRef = Mockito.mock(Ref.class);
        Clob mockClob = Mockito.mock(Clob.class);
        Array mockArray = Mockito.mock(Array.class);
        URL testURL = Mockito.mock(URL.class);
        RowId mockRowId = Mockito.mock(RowId.class);

        preparedStatementProxyXA.setRef(1, mockRef);
        preparedStatementProxyXA.setClob(2, mockClob);
        preparedStatementProxyXA.setArray(3, mockArray);
        preparedStatementProxyXA.setURL(4, testURL);
        preparedStatementProxyXA.setRowId(5, mockRowId);
        preparedStatementProxyXA.setNull(6, Types.VARCHAR, "VARCHAR");

        // Verify all calls were delegated
        Mockito.verify(mockPreparedStatement).setRef(1, mockRef);
        Mockito.verify(mockPreparedStatement).setClob(2, mockClob);
        Mockito.verify(mockPreparedStatement).setArray(3, mockArray);
        Mockito.verify(mockPreparedStatement).setURL(4, testURL);
        Mockito.verify(mockPreparedStatement).setRowId(5, mockRowId);
        Mockito.verify(mockPreparedStatement).setNull(6, Types.VARCHAR, "VARCHAR");
    }

    @Test
    public void testNationalCharacterSetters() throws SQLException {
        // Test national character set related setters
        Reader mockReader = Mockito.mock(Reader.class);
        NClob mockNClob = Mockito.mock(NClob.class);

        preparedStatementProxyXA.setNString(1, "国际化字符串");
        preparedStatementProxyXA.setNCharacterStream(2, mockReader, 100L);
        preparedStatementProxyXA.setNClob(3, mockNClob);
        preparedStatementProxyXA.setNCharacterStream(4, mockReader);
        preparedStatementProxyXA.setNClob(5, mockReader, 200L);
        preparedStatementProxyXA.setNClob(6, mockReader);

        // Verify all calls were delegated
        Mockito.verify(mockPreparedStatement).setNString(1, "国际化字符串");
        Mockito.verify(mockPreparedStatement).setNCharacterStream(2, mockReader, 100L);
        Mockito.verify(mockPreparedStatement).setNClob(3, mockNClob);
        Mockito.verify(mockPreparedStatement).setNCharacterStream(4, mockReader);
        Mockito.verify(mockPreparedStatement).setNClob(5, mockReader, 200L);
        Mockito.verify(mockPreparedStatement).setNClob(6, mockReader);
    }

    @Test
    public void testStreamSettersWithDifferentLengthParams() throws SQLException {
        // Test stream setters with different length parameter variants
        InputStream mockInputStream = Mockito.mock(InputStream.class);
        Reader mockReader = Mockito.mock(Reader.class);

        // Test long length variants
        preparedStatementProxyXA.setAsciiStream(1, mockInputStream, 1000L);
        preparedStatementProxyXA.setBinaryStream(2, mockInputStream, 2000L);
        preparedStatementProxyXA.setCharacterStream(3, mockReader, 3000L);

        // Test no-length variants
        preparedStatementProxyXA.setAsciiStream(4, mockInputStream);
        preparedStatementProxyXA.setBinaryStream(5, mockInputStream);
        preparedStatementProxyXA.setCharacterStream(6, mockReader);

        // Test Unicode stream (deprecated but still needs coverage)
        preparedStatementProxyXA.setUnicodeStream(7, mockInputStream, 500);

        // Verify all calls were delegated
        Mockito.verify(mockPreparedStatement).setAsciiStream(1, mockInputStream, 1000L);
        Mockito.verify(mockPreparedStatement).setBinaryStream(2, mockInputStream, 2000L);
        Mockito.verify(mockPreparedStatement).setCharacterStream(3, mockReader, 3000L);
        Mockito.verify(mockPreparedStatement).setAsciiStream(4, mockInputStream);
        Mockito.verify(mockPreparedStatement).setBinaryStream(5, mockInputStream);
        Mockito.verify(mockPreparedStatement).setCharacterStream(6, mockReader);
        Mockito.verify(mockPreparedStatement).setUnicodeStream(7, mockInputStream, 500);
    }

    @Test
    public void testBlobAndClobVariantSetters() throws SQLException {
        // Test various Blob and Clob setter variants
        InputStream mockInputStream = Mockito.mock(InputStream.class);
        Reader mockReader = Mockito.mock(Reader.class);

        // Test Blob variants
        preparedStatementProxyXA.setBlob(1, mockInputStream, 1000L);
        preparedStatementProxyXA.setBlob(2, mockInputStream);

        // Test Clob variants
        preparedStatementProxyXA.setClob(3, mockReader, 2000L);
        preparedStatementProxyXA.setClob(4, mockReader);

        // Verify all calls were delegated
        Mockito.verify(mockPreparedStatement).setBlob(1, mockInputStream, 1000L);
        Mockito.verify(mockPreparedStatement).setBlob(2, mockInputStream);
        Mockito.verify(mockPreparedStatement).setClob(3, mockReader, 2000L);
        Mockito.verify(mockPreparedStatement).setClob(4, mockReader);
    }

    @Test
    public void testSQLXMLSetter() throws SQLException {
        // Test SQLXML parameter setter
        SQLXML mockSQLXML = Mockito.mock(SQLXML.class);

        preparedStatementProxyXA.setSQLXML(1, mockSQLXML);

        // Verify call was delegated
        Mockito.verify(mockPreparedStatement).setSQLXML(1, mockSQLXML);
    }

    @Test
    public void testBatchAndParameterManagement() throws SQLException {
        // Test batch and parameter management methods
        preparedStatementProxyXA.addBatch();
        preparedStatementProxyXA.clearParameters();

        // Verify calls were delegated
        Mockito.verify(mockPreparedStatement).addBatch();
        Mockito.verify(mockPreparedStatement).clearParameters();
    }

    @Test
    public void testParameterSettersWithNullValues() throws SQLException {
        // Test parameter setters with null values where applicable
        preparedStatementProxyXA.setString(1, null);
        preparedStatementProxyXA.setBigDecimal(2, null);
        preparedStatementProxyXA.setDate(3, null);
        preparedStatementProxyXA.setTime(4, null);
        preparedStatementProxyXA.setTimestamp(5, null);
        preparedStatementProxyXA.setBytes(6, null);
        preparedStatementProxyXA.setRef(7, null);
        preparedStatementProxyXA.setBlob(8, (Blob) null);
        preparedStatementProxyXA.setClob(9, (Clob) null);
        preparedStatementProxyXA.setArray(10, null);
        preparedStatementProxyXA.setObject(11, null);

        // Verify all null values were handled correctly
        Mockito.verify(mockPreparedStatement).setString(1, null);
        Mockito.verify(mockPreparedStatement).setBigDecimal(2, null);
        Mockito.verify(mockPreparedStatement).setDate(3, null);
        Mockito.verify(mockPreparedStatement).setTime(4, null);
        Mockito.verify(mockPreparedStatement).setTimestamp(5, null);
        Mockito.verify(mockPreparedStatement).setBytes(6, null);
        Mockito.verify(mockPreparedStatement).setRef(7, null);
        Mockito.verify(mockPreparedStatement).setBlob(8, (Blob) null);
        Mockito.verify(mockPreparedStatement).setClob(9, (Clob) null);
        Mockito.verify(mockPreparedStatement).setArray(10, null);
        Mockito.verify(mockPreparedStatement).setObject(11, null);
    }

    @Test
    public void testParameterSettersThrowSQLException() throws SQLException {
        // Test that SQL exceptions from parameter setters are properly propagated
        SQLException expectedException = new SQLException("Parameter setting failed");

        Mockito.doThrow(expectedException).when(mockPreparedStatement).setString(1, "test");

        SQLException actualException = Assertions.assertThrows(SQLException.class, () -> {
            preparedStatementProxyXA.setString(1, "test");
        });

        Assertions.assertSame(
                expectedException,
                actualException,
                "SQLException from parameter setter should be propagated without modification");
    }

    @Test
    public void testMetaDataMethodsThrowSQLException() throws SQLException {
        // Test that SQL exceptions from metadata methods are properly propagated
        SQLException expectedException = new SQLException("Metadata access failed");

        Mockito.when(mockPreparedStatement.getMetaData()).thenThrow(expectedException);

        SQLException actualException = Assertions.assertThrows(SQLException.class, () -> {
            preparedStatementProxyXA.getMetaData();
        });

        Assertions.assertSame(
                expectedException,
                actualException,
                "SQLException from getMetaData should be propagated without modification");
    }

    @Test
    public void testExecuteUpdatePropagatesException() {
        // Test that exceptions from executeUpdate are properly propagated
        SQLException expectedException = new SQLException("Update execution failed");

        try (MockedStatic<ExecuteTemplateXA> mockedExecuteTemplate = Mockito.mockStatic(ExecuteTemplateXA.class)) {
            mockedExecuteTemplate
                    .when(() -> ExecuteTemplateXA.execute(
                            Mockito.eq(mockConnectionProxyXA), Mockito.any(), Mockito.eq(mockPreparedStatement)))
                    .thenThrow(expectedException);

            SQLException actualException = Assertions.assertThrows(SQLException.class, () -> {
                preparedStatementProxyXA.executeUpdate();
            });

            Assertions.assertSame(
                    expectedException,
                    actualException,
                    "Exception from executeUpdate should be propagated without modification");
        }
    }

    @Test
    public void testExecutePropagatesException() {
        // Test that exceptions from execute are properly propagated
        SQLException expectedException = new SQLException("Execute failed");

        try (MockedStatic<ExecuteTemplateXA> mockedExecuteTemplate = Mockito.mockStatic(ExecuteTemplateXA.class)) {
            mockedExecuteTemplate
                    .when(() -> ExecuteTemplateXA.execute(
                            Mockito.eq(mockConnectionProxyXA), Mockito.any(), Mockito.eq(mockPreparedStatement)))
                    .thenThrow(expectedException);

            SQLException actualException = Assertions.assertThrows(SQLException.class, () -> {
                preparedStatementProxyXA.execute();
            });

            Assertions.assertSame(
                    expectedException,
                    actualException,
                    "Exception from execute should be propagated without modification");
        }
    }

    @Test
    public void testComprehensiveParameterSettingWithExecution() throws SQLException {
        // Comprehensive test setting various parameters and then executing successfully
        InputStream mockInputStream = Mockito.mock(InputStream.class);
        Reader mockReader = Mockito.mock(Reader.class);
        Blob mockBlob = Mockito.mock(Blob.class);
        Clob mockClob = Mockito.mock(Clob.class);
        Array mockArray = Mockito.mock(Array.class);
        Ref mockRef = Mockito.mock(Ref.class);
        ResultSet expectedResultSet = Mockito.mock(ResultSet.class);

        // Set a comprehensive set of parameters
        preparedStatementProxyXA.setBoolean(1, false);
        preparedStatementProxyXA.setByte(2, (byte) -128);
        preparedStatementProxyXA.setShort(3, (short) -32768);
        preparedStatementProxyXA.setInt(4, -2147483648);
        preparedStatementProxyXA.setLong(5, -9223372036854775808L);
        preparedStatementProxyXA.setFloat(6, Float.MIN_VALUE);
        preparedStatementProxyXA.setDouble(7, Double.MIN_VALUE);
        preparedStatementProxyXA.setBigDecimal(8, new BigDecimal("-999999.999999"));
        preparedStatementProxyXA.setString(9, "测试中文字符串");
        preparedStatementProxyXA.setBytes(10, new byte[] {-1, 0, 1});
        preparedStatementProxyXA.setDate(11, new Date(0));
        preparedStatementProxyXA.setTime(12, new Time(0));
        preparedStatementProxyXA.setTimestamp(13, new Timestamp(0));
        preparedStatementProxyXA.setAsciiStream(14, mockInputStream);
        preparedStatementProxyXA.setBinaryStream(15, mockInputStream);
        preparedStatementProxyXA.setCharacterStream(16, mockReader);
        preparedStatementProxyXA.setObject(17, "testObject");
        preparedStatementProxyXA.setBlob(18, mockBlob);
        preparedStatementProxyXA.setClob(19, mockClob);
        preparedStatementProxyXA.setArray(20, mockArray);
        preparedStatementProxyXA.setRef(21, mockRef);
        preparedStatementProxyXA.setNull(22, Types.INTEGER);

        // Execute query and verify success
        try (MockedStatic<ExecuteTemplateXA> mockedExecuteTemplate = Mockito.mockStatic(ExecuteTemplateXA.class)) {
            mockedExecuteTemplate
                    .when(() -> ExecuteTemplateXA.execute(
                            Mockito.eq(mockConnectionProxyXA), Mockito.any(), Mockito.eq(mockPreparedStatement)))
                    .thenReturn(expectedResultSet);

            ResultSet actualResultSet = preparedStatementProxyXA.executeQuery();

            Assertions.assertSame(
                    expectedResultSet,
                    actualResultSet,
                    "Query should execute successfully after setting comprehensive parameters");
        }
    }
}
