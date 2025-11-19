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
package org.apache.seata.rm.datasource.util;

import org.apache.seata.rm.BaseDataSourceResource;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.sqlparser.util.DbTypeParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class JdbcUtilsTest {

    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/test";
    private static final String MYSQL_URL_WITH_PARAMS =
            "jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC";
    private static final String ORACLE_URL = "jdbc:oracle:thin:@localhost:1521:orcl";
    private static final String POSTGRESQL_URL = "jdbc:postgresql://localhost:5432/test";
    private static final String H2_URL = "jdbc:h2:mem:test";

    private MockedStatic<DefaultResourceManager> mockedResourceManager;
    private DefaultResourceManager resourceManager;

    @BeforeEach
    public void setUp() {
        resourceManager = mock(DefaultResourceManager.class);
        mockedResourceManager = Mockito.mockStatic(DefaultResourceManager.class);
        mockedResourceManager.when(DefaultResourceManager::get).thenReturn(resourceManager);
    }

    @AfterEach
    public void tearDown() {
        if (mockedResourceManager != null) {
            mockedResourceManager.close();
        }
    }

    @Test
    public void testDbTypeParserLoading() {
        DbTypeParser dbTypeParser = JdbcUtils.getDbTypeParser();
        Assertions.assertNotNull(dbTypeParser);
    }

    @Test
    public void testDbTypeParserSingletonPattern() {
        DbTypeParser first = JdbcUtils.getDbTypeParser();
        DbTypeParser second = JdbcUtils.getDbTypeParser();
        assertSame(first, second, "DbTypeParser should be singleton");
    }

    @Test
    public void testGetDbTypeWithMySQLUrl() {
        String dbType = JdbcUtils.getDbType(MYSQL_URL);
        assertEquals("mysql", dbType);
    }

    @Test
    public void testGetDbTypeWithOracleUrl() {
        String dbType = JdbcUtils.getDbType(ORACLE_URL);
        assertEquals("oracle", dbType);
    }

    @Test
    public void testGetDbTypeWithPostgreSQLUrl() {
        String dbType = JdbcUtils.getDbType(POSTGRESQL_URL);
        assertEquals("postgresql", dbType);
    }

    @Test
    public void testGetDbTypeWithH2Url() {
        String dbType = JdbcUtils.getDbType(H2_URL);
        assertEquals("h2", dbType);
    }

    @Test
    public void testBuildResourceIdWithQueryString() {
        String resourceId = JdbcUtils.buildResourceId(MYSQL_URL_WITH_PARAMS);
        assertEquals(MYSQL_URL, resourceId, "Should remove query string");
    }

    @Test
    public void testBuildResourceIdWithoutQueryString() {
        String resourceId = JdbcUtils.buildResourceId(MYSQL_URL);
        assertEquals(MYSQL_URL, resourceId, "Should keep URL unchanged");
    }

    @Test
    public void testBuildResourceIdWithEmptyQueryString() {
        String urlWithEmptyQuery = "jdbc:mysql://localhost:3306/test?";
        String resourceId = JdbcUtils.buildResourceId(urlWithEmptyQuery);
        assertEquals(MYSQL_URL, resourceId, "Should remove trailing question mark");
    }

    @Test
    public void testBuildResourceIdWithMultipleQuestionMarks() {
        String urlWithMultiple = "jdbc:mysql://localhost:3306/test?param1=value1?param2=value2";
        String resourceId = JdbcUtils.buildResourceId(urlWithMultiple);
        assertEquals(MYSQL_URL, resourceId, "Should remove from first question mark");
    }

    @Test
    public void testLoadDriverWithValidH2Driver() throws SQLException {
        Driver driver = JdbcUtils.loadDriver("org.h2.Driver");
        assertNotNull(driver, "Should load H2 driver successfully");
        assertTrue(driver instanceof org.h2.Driver);
    }

    @Test
    public void testLoadDriverWithInvalidDriverClass() {
        SQLException exception = assertThrows(SQLException.class, () -> {
            JdbcUtils.loadDriver("com.invalid.NonExistentDriver");
        });
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof ClassNotFoundException);
    }

    @Test
    public void testLoadDriverWithNullClassName() {
        assertThrows(Exception.class, () -> {
            JdbcUtils.loadDriver(null);
        });
    }

    @Test
    public void testLoadDriverWithEmptyClassName() {
        assertThrows(Exception.class, () -> {
            JdbcUtils.loadDriver("");
        });
    }

    @Test
    public void testInitDataSourceResourceSuccess() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        BaseDataSourceResource resource = mock(BaseDataSourceResource.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getURL()).thenReturn(MYSQL_URL_WITH_PARAMS);

        JdbcUtils.initDataSourceResource(resource, dataSource, "test-group");

        verify(resource).setResourceGroupId("test-group");
        verify(resource).setResourceId(MYSQL_URL);
        verify(resource).setDbType("mysql");
        verify(resource).setDriver(any(Driver.class));
        verify(resourceManager).registerResource(resource);
        verify(connection).close();
    }

    @Test
    public void testInitDataSourceResourceWithSQLException() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        BaseDataSourceResource resource = mock(BaseDataSourceResource.class);

        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            JdbcUtils.initDataSourceResource(resource, dataSource, "test-group");
        });

        assertTrue(exception.getMessage().contains("can not init DataSourceResource"));
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof SQLException);
        verify(resource).setResourceGroupId("test-group");
        verify(resourceManager, never()).registerResource(any());
    }

    @Test
    public void testInitDataSourceResourceWithNullMetaData() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        BaseDataSourceResource resource = mock(BaseDataSourceResource.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(null);

        assertThrows(Exception.class, () -> {
            JdbcUtils.initDataSourceResource(resource, dataSource, "test-group");
        });

        verify(connection).close();
    }

    @Test
    public void testInitXADataSourceResourceSuccess() throws SQLException {
        XADataSource xaDataSource = mock(XADataSource.class);
        XAConnection xaConnection = mock(XAConnection.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        BaseDataSourceResource resource = mock(BaseDataSourceResource.class);

        when(xaDataSource.getXAConnection()).thenReturn(xaConnection);
        when(xaConnection.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getURL()).thenReturn(H2_URL);

        JdbcUtils.initXADataSourceResource(resource, xaDataSource, "xa-group");

        verify(resource).setResourceGroupId("xa-group");
        verify(resource).setResourceId(H2_URL);
        verify(resource).setDbType("h2");
        verify(resource).setDriver(any(Driver.class));
        verify(resourceManager).registerResource(resource);
        verify(connection).close();
        verify(xaConnection).close();
    }

    @Test
    public void testInitXADataSourceResourceWithSQLException() throws SQLException {
        XADataSource xaDataSource = mock(XADataSource.class);
        BaseDataSourceResource resource = mock(BaseDataSourceResource.class);

        when(xaDataSource.getXAConnection()).thenThrow(new SQLException("XA connection failed"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            JdbcUtils.initXADataSourceResource(resource, xaDataSource, "xa-group");
        });

        assertTrue(exception.getMessage().contains("can not get XAConnection"));
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof SQLException);
        verify(resource).setResourceGroupId("xa-group");
        verify(resourceManager, never()).registerResource(any());
    }

    @Test
    public void testInitXADataSourceResourceWithNullXAConnection() throws SQLException {
        XADataSource xaDataSource = mock(XADataSource.class);
        BaseDataSourceResource resource = mock(BaseDataSourceResource.class);

        when(xaDataSource.getXAConnection()).thenReturn(null);

        assertThrows(Exception.class, () -> {
            JdbcUtils.initXADataSourceResource(resource, xaDataSource, "xa-group");
        });

        verify(resource).setResourceGroupId("xa-group");
        verify(resourceManager, never()).registerResource(any());
    }

    @Test
    public void testInitXADataSourceResourceEnsuresConnectionClosed() throws SQLException {
        XADataSource xaDataSource = mock(XADataSource.class);
        XAConnection xaConnection = mock(XAConnection.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        BaseDataSourceResource resource = mock(BaseDataSourceResource.class);

        when(xaDataSource.getXAConnection()).thenReturn(xaConnection);
        when(xaConnection.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getURL()).thenReturn(H2_URL);
        doThrow(new SQLException("Close failed")).when(xaConnection).close();

        try {
            JdbcUtils.initXADataSourceResource(resource, xaDataSource, "xa-group");
        } catch (Exception e) {
            // Expected
        }

        verify(connection).close();
        verify(xaConnection).close();
    }
}
