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
package org.apache.seata.mcp.store;

import org.apache.seata.common.exception.StoreException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SqlExecutionTemplateTest {

    @InjectMocks
    private SqlExecutionTemplate sqlExecutionTemplate;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ResultSetMetaData resultSetMetaData;

    private final String resourceId = "testResourceId";
    private final String selectSql = "SELECT * FROM test_table WHERE id = ?";
    private final String invalidSql = "INVALID SQL";

    // MockedStatic object at the class level
    private MockedStatic<DataSourceFactory> dataSourceFactoryMock;

    @BeforeEach
    public void setUp() throws SQLException {
        // Initialize MockedStatic and remain valid for the entire duration of the test
        dataSourceFactoryMock = Mockito.mockStatic(DataSourceFactory.class);
        dataSourceFactoryMock
                .when(() -> DataSourceFactory.getDataSource(resourceId))
                .thenReturn(dataSource);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    }

    @AfterEach
    public void tearDown() {
        // Turn off MockedStatic when the test is over
        if (dataSourceFactoryMock != null) {
            dataSourceFactoryMock.close();
        }
    }

    @Test
    public void testQuery_Success() throws SQLException {
        // Set up metadata and result sets
        when(resultSetMetaData.getColumnCount()).thenReturn(2);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("id");
        when(resultSetMetaData.getColumnLabel(2)).thenReturn("name");

        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getObject(1)).thenReturn(1, 2);
        when(resultSet.getObject(2)).thenReturn("test1", "test2");

        List<Map<String, Object>> result = sqlExecutionTemplate.query(resourceId, selectSql, 1);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).get("id"));
        assertEquals("test1", result.get(0).get("name"));
        assertEquals(2, result.get(1).get("id"));
        assertEquals("test2", result.get(1).get("name"));

        verify(preparedStatement).setObject(1, 1);
        verify(preparedStatement).executeQuery();
    }

    @Test
    public void testQuery_InvalidSql() {
        // test NON-SELECT SENTENCE
        assertThrows(StoreException.class, () -> sqlExecutionTemplate.query(resourceId, invalidSql));
    }

    @Test
    public void testQuery_SQLException() throws SQLException {
        // simulation SQLException
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("Test SQLException"));

        assertThrows(StoreException.class, () -> sqlExecutionTemplate.query(resourceId, selectSql));
    }

    @Test
    public void testQueryForObject_Success() throws SQLException {
        when(resultSetMetaData.getColumnCount()).thenReturn(2);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("id");
        when(resultSetMetaData.getColumnLabel(2)).thenReturn("name");

        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject(1)).thenReturn(1);
        when(resultSet.getObject(2)).thenReturn("test1");

        Map<String, Object> result = sqlExecutionTemplate.queryForObject(resourceId, selectSql, 1);

        assertNotNull(result);
        assertEquals(1, result.get("id"));
        assertEquals("test1", result.get("name"));
    }

    @Test
    public void testQueryForObject_NoResults() throws SQLException {
        // Set an empty result set
        when(resultSetMetaData.getColumnCount()).thenReturn(2);
        when(resultSet.next()).thenReturn(false);

        Map<String, Object> result = sqlExecutionTemplate.queryForObject(resourceId, selectSql, 1);

        assertNull(result);
    }

    @Test
    public void testGetDataSource_Exception() {
        // Simulated an exception to obtain a data source
        dataSourceFactoryMock
                .when(() -> DataSourceFactory.getDataSource("invalidId"))
                .thenThrow(new RuntimeException("DataSource not found"));

        // Verify that exceptions are properly packaged
        assertThrows(StoreException.class, () -> sqlExecutionTemplate.query("invalidId", selectSql));
    }

    @Test
    public void testValidateQuerySql() {
        // Test valid query statements
        assertDoesNotThrow(() -> sqlExecutionTemplate.query(resourceId, "SELECT * FROM table"));

        // Test invalid query statements
        assertThrows(StoreException.class, () -> sqlExecutionTemplate.query(resourceId, null));

        assertThrows(StoreException.class, () -> sqlExecutionTemplate.query(resourceId, ""));

        assertThrows(StoreException.class, () -> sqlExecutionTemplate.query(resourceId, "INSERT INTO table VALUES(1)"));
    }
}
