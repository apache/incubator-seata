/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.rm.datasource.exec;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.exec.mysql.MySQLInsertExecutor;
import io.seata.rm.datasource.mock.MockMariadbDataSource;
import io.seata.rm.datasource.mock.MockResultSet;
import io.seata.sqlparser.struct.TableMeta;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author funkye
 */
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

        MockResultSet resultSet = new MockResultSet(statementProxy);
        resultSet.mockResultSet(Arrays.asList("Variable_name", "Value"), new Object[][]{{"auto_increment_increment", "1"}});
        when(statementProxy.getTargetStatement().executeQuery("SHOW VARIABLES LIKE 'auto_increment_increment'")).thenReturn(resultSet);

        StatementCallback statementCallback = mock(StatementCallback.class);
        sqlInsertRecognizer = mock(SQLInsertRecognizer.class);
        tableMeta = mock(TableMeta.class);
        insertExecutor = Mockito.spy(new MySQLInsertExecutor(statementProxy, statementCallback, sqlInsertRecognizer));

        pkIndexMap = new HashMap<String,Integer>(){
            {
                put(ID_COLUMN, pkIndex);
            }
        };
    }
}
