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
package org.apache.seata.rm.datasource.exec;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;

import com.alibaba.druid.mock.MockStatement;
import com.alibaba.druid.mock.MockStatementBase;
import com.alibaba.druid.pool.DruidDataSource;
import com.google.common.collect.Lists;
import org.apache.seata.rm.datasource.ConnectionProxy;
import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.DataSourceProxyTest;
import org.apache.seata.rm.datasource.PreparedStatementProxy;
import org.apache.seata.rm.datasource.StatementProxy;
import org.apache.seata.rm.datasource.exec.mysql.MySQLInsertExecutor;
import org.apache.seata.rm.datasource.mock.MockDriver;
import org.apache.seata.rm.datasource.mock.MockMariadbDataSource;
import org.apache.seata.sqlparser.SQLInsertRecognizer;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class MariadbInsertExecutorTest extends MySQLInsertExecutorTest {

    @BeforeEach
    @Override
    public void init() throws SQLException {
        ConnectionProxy connectionProxy = mock(ConnectionProxy.class);
        when(connectionProxy.getDbType()).thenReturn(JdbcConstants.MARIADB);
        DataSourceProxy dataSourceProxy = new DataSourceProxy(new MockMariadbDataSource());
        when(connectionProxy.getDataSourceProxy()).thenReturn(dataSourceProxy);
        Assertions.assertEquals(JdbcConstants.MARIADB, dataSourceProxy.getDbType());
        statementProxy = mock(PreparedStatementProxy.class);
        when(statementProxy.getConnectionProxy()).thenReturn(connectionProxy);
        when(statementProxy.getTargetStatement()).thenReturn(statementProxy);

        StatementCallback statementCallback = mock(StatementCallback.class);
        sqlInsertRecognizer = mock(SQLInsertRecognizer.class);
        tableMeta = mock(TableMeta.class);
        insertExecutor = Mockito.spy(new MySQLInsertExecutor(statementProxy, statementCallback, sqlInsertRecognizer));

        pkIndexMap = new HashMap<String,Integer>(){
            {
                put(ID_COLUMN, pkIndex);
            }
        };

        // new test init property
        List<String> returnValueColumnLabels = Lists.newArrayList("id", "user_id", "name", "sex", "update_time");
        Object[][] returnValue = new Object[][] {
                new Object[] {1, 1, "will", 1, 0},
        };
        Object[][] columnMetas = new Object[][] {
                new Object[] {"", "", "table_insert_executor_test", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 2, "NO", "NO"},
                new Object[] {"", "", "table_insert_executor_test", "user_id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 2, "NO", "NO"},
                new Object[] {"", "", "table_insert_executor_test", "name", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "NO", "NO"},
                new Object[] {"", "", "table_insert_executor_test", "sex", Types.INTEGER, "INTEGER", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "NO", "NO"},
                new Object[] {"", "", "table_insert_executor_test", "update_time", Types.INTEGER, "INTEGER", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
        };
        Object[][] indexMetas = new Object[][] {
                new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},
                new Object[] {"PRIMARY", "user_id", false, "", 3, 1, "A", 34},
        };
        Object[][] onUpdateColumnsReturnValue = new Object[][] {
                new Object[]{0, "update_time", Types.INTEGER, "INTEGER", 64, 10, 0, 0}
        };

        MockDriver mockDriver = new MockDriver(returnValueColumnLabels, returnValue, columnMetas, indexMetas, null, onUpdateColumnsReturnValue, new Object[][]{});
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx2");
        dataSource.setDriver(mockDriver);

        DataSourceProxy newDataSourceProxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        try {
            Field field = dataSourceProxy.getClass().getDeclaredField("dbType");
            field.setAccessible(true);
            field.set(newDataSourceProxy, "mysql");
            ConnectionProxy newConnectionProxy = new ConnectionProxy(newDataSourceProxy, dataSource.getConnection().getConnection());
            MockStatementBase mockStatement = new MockStatement(dataSource.getConnection().getConnection());
            newStatementProxy = new StatementProxy(newConnectionProxy, mockStatement);
        } catch (Exception e) {
            throw new RuntimeException("init failed");
        }
    }
}
