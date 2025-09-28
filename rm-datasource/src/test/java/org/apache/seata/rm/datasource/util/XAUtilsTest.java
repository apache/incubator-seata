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

import com.alibaba.druid.util.MySqlUtils;
import com.alibaba.druid.util.PGUtils;
import org.apache.seata.rm.BaseDataSourceResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;

import static org.apache.seata.sqlparser.util.JdbcConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class XAUtilsTest {
    private Connection mockConnection;
    private Driver mockDriver;
    private BaseDataSourceResource mockDataSourceResource;

    @BeforeEach
    public void setUp() {
        mockConnection = mock(Connection.class);
        mockDriver = mock(Driver.class);
        mockDataSourceResource = mock(BaseDataSourceResource.class);
        when(mockDataSourceResource.getDriver()).thenReturn(mockDriver);
    }

    @Test
    public void testCreateXAConnectionMySQL() throws SQLException {
        when(mockDataSourceResource.getDbType()).thenReturn(MYSQL);
        XAConnection mockXAConnection = mock(XAConnection.class);

        try (MockedStatic<MySqlUtils> mySqlUtilsMock = Mockito.mockStatic(MySqlUtils.class)) {
            mySqlUtilsMock
                    .when(() -> MySqlUtils.createXAConnection(any(), any()))
                    .thenReturn(mockXAConnection);
            XAConnection result = XAUtils.createXAConnection(mockConnection, mockDataSourceResource);
            assertSame(mockXAConnection, result);
        }
    }

    @Test
    public void testCreateXAConnectionPostgreSQL() throws SQLException, ClassNotFoundException {
        when(mockDataSourceResource.getDbType()).thenReturn(POSTGRESQL);
        XAConnection mockXAConnection = mock(XAConnection.class);
        try (MockedStatic<PGUtils> pgUtilsMock = Mockito.mockStatic(PGUtils.class)) {
            pgUtilsMock.when(() -> PGUtils.createXAConnection(any())).thenReturn(mockXAConnection);
            XAConnection result = XAUtils.createXAConnection(mockConnection, mockDataSourceResource);
            assertSame(mockXAConnection, result);
        }
    }

    private void testCreateXAConnectionForDbType(String dbType, String connectionClass, String xaConnectionClass) {
        try {
            when(mockDataSourceResource.getDbType()).thenReturn(dbType);

            Connection specificConn = mock(Class.forName(connectionClass).asSubclass(Connection.class));

            try (MockedConstruction<?> xaConstruction = mockConstruction(
                    Class.forName(xaConnectionClass).asSubclass(XAConnection.class), (mock, context) -> {
                        Connection param = (Connection) context.arguments().get(0);
                        assertSame(specificConn, param);
                    })) {

                XAConnection result = XAUtils.createXAConnection(specificConn, mockDataSourceResource);
                assertNotNull(result);
            }
        } catch (Exception e) {
            fail(dbType + " test failed: " + e.getMessage());
        }
    }

    @Test
    public void testCreateXAConnectionMariaDB() throws SQLException, ClassNotFoundException {
        testCreateXAConnectionForDbType(
                MARIADB, "org.mariadb.jdbc.MariaDbConnection", "org.mariadb.jdbc.MariaXaConnection");
    }

    @Test
    public void testCreateXAConnectionKingbase() throws SQLException, ClassNotFoundException {
        testCreateXAConnectionForDbType(
                KINGBASE, "com.kingbase8.core.BaseConnection", "com.kingbase8.xa.KBXAConnection");
    }

    @Test
    public void testCreateXAConnectionDM() throws SQLException, ClassNotFoundException {
        testCreateXAConnectionForDbType(DM, "dm.jdbc.driver.DmdbConnection", "dm.jdbc.driver.DmdbXAConnection");
    }

    @Test
    public void testCreateXAConnectionOscar() throws SQLException, ClassNotFoundException {
        testCreateXAConnectionForDbType(OSCAR, "com.oscar.jdbc.OscarJdbc2Connection", "com.oscar.xa.Jdbc3XAConnection");
    }
}
