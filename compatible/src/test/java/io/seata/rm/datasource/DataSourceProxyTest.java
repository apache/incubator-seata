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
package io.seata.rm.datasource;

import java.sql.Connection;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSource;

import io.seata.rm.datasource.mock.MockDataSource;
import io.seata.rm.datasource.mock.MockDriver;
import org.apache.seata.rm.datasource.undo.UndoLogManagerFactory;
import org.apache.seata.rm.datasource.undo.mysql.MySQLUndoLogManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class DataSourceProxyTest {

    @Test
    public void test_constructor() {
        DataSource dataSource = new MockDataSource();
        DataSourceProxy dataSourceProxy = new DataSourceProxy(dataSource);
        Assertions.assertEquals(dataSourceProxy.getTargetDataSource(), dataSource);

        DataSourceProxy dataSourceProxy2 = new DataSourceProxy(dataSourceProxy);
        Assertions.assertEquals(dataSourceProxy2.getTargetDataSource(), dataSource);
    }

    @Test
    public void testNotSupportDb() {
        final MockDriver mockDriver = new MockDriver();
        final String username = "username";
        final String jdbcUrl = "jdbc:mock:xxx";

        // create data source
        final DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setDriver(mockDriver);
        dataSource.setUsername(username);
        dataSource.setPassword("password");

        Throwable throwable = Assertions.assertThrows(IllegalStateException.class, () -> new DataSourceProxy(dataSource));
        assertThat(throwable).hasMessageContaining("AT mode don't support the dbtype");
    }


    @Test
    public void testUndologTableNotExist() {
        DataSource dataSource = new MockDataSource();

        MockedStatic<UndoLogManagerFactory> undoLogManagerFactoryMockedStatic = Mockito.mockStatic(UndoLogManagerFactory.class);

        MySQLUndoLogManager mysqlUndoLogManager = mock(MySQLUndoLogManager.class);
        undoLogManagerFactoryMockedStatic.when(()->UndoLogManagerFactory.getUndoLogManager(anyString()))
                .thenReturn(mysqlUndoLogManager);

        doReturn(false).when(mysqlUndoLogManager).hasUndoLogTable(any(Connection.class));

        Throwable throwable = Assertions.assertThrows(IllegalStateException.class, () -> new DataSourceProxy(dataSource));
        undoLogManagerFactoryMockedStatic.close();

        assertThat(throwable).hasMessageContaining("table not exist");
    }

    // to skip the db & undolog table check
    public static DataSourceProxy getDataSourceProxy(DataSource dataSource) {
        try (MockedStatic<UndoLogManagerFactory> undoLogManagerFactoryMockedStatic = Mockito.mockStatic(UndoLogManagerFactory.class)) {
            MySQLUndoLogManager mysqlUndoLogManager = mock(MySQLUndoLogManager.class);
            undoLogManagerFactoryMockedStatic.when(() -> UndoLogManagerFactory.getUndoLogManager(anyString())).thenReturn(mysqlUndoLogManager);

            doReturn(true).when(mysqlUndoLogManager).hasUndoLogTable(any(Connection.class));

            // create data source proxy
            return new DataSourceProxy(dataSource);
        }
    }
}
