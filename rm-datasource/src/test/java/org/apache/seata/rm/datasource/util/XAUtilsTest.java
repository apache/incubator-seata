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


    @Test
    public void testCreateXAConnectionMariaDB() throws SQLException, ClassNotFoundException {
        when(mockDataSourceResource.getDbType()).thenReturn(MARIADB);
        // 模拟MariaDB特定的连接类
        Class<?> mariaDbConnectionClass = Class.forName("org.mariadb.jdbc.MariaDbConnection");
        Connection mariaMockConnection = mock(mariaDbConnectionClass.asSubclass(Connection.class));
        try (MockedConstruction<?> ignored = mockConstruction(
                Class.forName("org.mariadb.jdbc.MariaXaConnection").asSubclass(XAConnection.class),
                (mock, context) -> {
                    // 验证构造器参数类型
                    Connection connectionParam = (Connection) context.arguments().get(0);
                    assertSame(mariaMockConnection, connectionParam);
                })) {

            XAConnection result = XAUtils.createXAConnection(mariaMockConnection, mockDataSourceResource);
            assertNotNull(result);
        } catch (ClassNotFoundException e) {
            fail("MariaDB XAConnection class not found in test environment");
        }
    }

    @Test
    public void testCreateXAConnectionKingbase() throws SQLException, ClassNotFoundException {
        when(mockDataSourceResource.getDbType()).thenReturn(KINGBASE);
        // 模拟Kingbase特定的连接类
        Class<?> kingbaseConnectionClass = Class.forName("com.kingbase8.core.BaseConnection");
        Connection kingbaseMockConnection = mock(kingbaseConnectionClass.asSubclass(Connection.class));
        try (MockedConstruction<?> ignored = mockConstruction(
                Class.forName("com.kingbase8.xa.KBXAConnection").asSubclass(XAConnection.class),
                (mock, context) -> {
                    // 验证构造器参数类型
                    Connection connectionParam = (Connection) context.arguments().get(0);
                    assertSame(kingbaseMockConnection, connectionParam);
                })) {
            XAConnection result = XAUtils.createXAConnection(kingbaseMockConnection, mockDataSourceResource);
            assertNotNull(result);
        } catch (ClassNotFoundException e) {
            fail("Kingbase XAConnection class not found in test environment");
        }
    }

    @Test
    public void testCreateXAConnectionDM() throws SQLException, ClassNotFoundException {
        when(mockDataSourceResource.getDbType()).thenReturn(DM);
        // 模拟达梦特定的连接类
        Class<?> dmConnectionClass = Class.forName("dm.jdbc.driver.DmdbConnection");
        Connection dmMockConnection = mock(dmConnectionClass.asSubclass(Connection.class));
        try (MockedConstruction<?> ignored = mockConstruction(
                Class.forName("dm.jdbc.driver.DmdbXAConnection").asSubclass(XAConnection.class),
                (mock, context) -> {
                    // 验证构造器参数类型
                    Connection connectionParam = (Connection) context.arguments().get(0);
                    assertSame(dmMockConnection, connectionParam);
                })) {
            XAConnection result = XAUtils.createXAConnection(dmMockConnection, mockDataSourceResource);
            assertNotNull(result);
        } catch (ClassNotFoundException e) {
            fail("DM XAConnection class not found in test environment");
        }
    }
}
