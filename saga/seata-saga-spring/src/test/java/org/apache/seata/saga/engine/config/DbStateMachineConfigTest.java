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
package org.apache.seata.saga.engine.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


import static org.mockito.Mockito.when;

/**
 * DbStateMachineConfigTest
 */
public class DbStateMachineConfigTest {
    @Test
    public void testGetDbTypeFromDataSource() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        DatabaseMetaData databaseMetaData = Mockito.mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("test");
        MockDataSource mockDataSource = new MockDataSource();
        mockDataSource.setConnection(connection);
        Assertions.assertEquals(DbStateMachineConfig.getDbTypeFromDataSource(mockDataSource), "test");
    }

    @Test
    public void testAfterPropertiesSet() throws Exception {
        DbStateMachineConfig dbStateMachineConfig = new DbStateMachineConfig();
        Connection connection = Mockito.mock(Connection.class);
        DatabaseMetaData databaseMetaData = Mockito.mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("test");
        MockDataSource mockDataSource = new MockDataSource();
        mockDataSource.setConnection(connection);
        dbStateMachineConfig.setDataSource(mockDataSource);
        dbStateMachineConfig.setApplicationId("test");
        dbStateMachineConfig.setTxServiceGroup("test");

        Assertions.assertDoesNotThrow(dbStateMachineConfig::afterPropertiesSet);
    }
}