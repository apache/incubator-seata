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
import java.sql.Blob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
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
}
