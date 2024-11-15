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
import java.util.List;

import com.alibaba.druid.mock.MockStatement;
import com.alibaba.druid.mock.MockStatementBase;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;
import com.google.common.collect.Lists;
import org.apache.seata.rm.datasource.ConnectionProxy;
import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.DataSourceProxyTest;
import org.apache.seata.rm.datasource.StatementProxy;
import org.apache.seata.rm.datasource.exec.UpdateExecutor;
import org.apache.seata.rm.datasource.exec.mysql.MySQLUpdateJoinExecutor;
import org.apache.seata.rm.datasource.mock.MockDriver;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.sqlparser.druid.mysql.MySQLUpdateRecognizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class UpdateJoinExecutorTest {
    @Test
    public void testUpdateJoinUndoLog() throws SQLException {
        List<String> returnValueColumnLabels = Lists.newArrayList("id", "name");
        Object[][] columnMetas = new Object[][]{
                new Object[]{"", "", "t1", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
                new Object[]{"", "", "t1", "name", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                new Object[]{"", "", "t2", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
                new Object[]{"", "", "t2", "name", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                new Object[]{"", "", "t1 inner join t2 on t1.id = t2.id", "id", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                new Object[]{"", "", "t1 inner join t2 on t1.id = t2.id", "name", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
        };
        Object[][] indexMetas = new Object[][]{
                new Object[]{"PRIMARY", "id", false, "", 3, 1, "A", 34},
        };
        Object[][] beforeReturnValue = new Object[][]{
                new Object[]{1, "Tom"},
        };
        StatementProxy beforeMockStatementProxy = mockStatementProxy(returnValueColumnLabels, beforeReturnValue, columnMetas, indexMetas);
        String sql = "update t1 inner join t2 on t1.id = t2.id set t1.name = 'WILL',t2.name = 'WILL'";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        MySQLUpdateRecognizer recognizer = new MySQLUpdateRecognizer(sql, asts.get(0));
        UpdateExecutor mySQLUpdateJoinExecutor = new MySQLUpdateJoinExecutor(beforeMockStatementProxy, (statement, args) -> {
            return null;
        }, recognizer);
        TableRecords beforeImage = mySQLUpdateJoinExecutor.beforeImage();
        Object[][] afterReturnValue = new Object[][]{
                new Object[]{1, "WILL"},
        };
        StatementProxy afterMockStatementProxy = mockStatementProxy(returnValueColumnLabels, afterReturnValue, columnMetas, indexMetas);
        mySQLUpdateJoinExecutor.statementProxy = afterMockStatementProxy;
        TableRecords afterImage = mySQLUpdateJoinExecutor.afterImage(beforeImage);
        Assertions.assertDoesNotThrow(()->mySQLUpdateJoinExecutor.prepareUndoLog(beforeImage, afterImage));
    }

    @Test
    public void testEmptyUpdateJoinUndoLog() throws SQLException {
        List<String> returnValueColumnLabels = Lists.newArrayList("id", "name");
        Object[][] columnMetas = new Object[][]{
                new Object[]{"", "", "t1", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
                new Object[]{"", "", "t1", "name", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                new Object[]{"", "", "t2", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
                new Object[]{"", "", "t2", "name", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                new Object[]{"", "", "t1 inner join t2 on t1.id = t2.id", "id", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                new Object[]{"", "", "t1 inner join t2 on t1.id = t2.id", "name", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
        };
        Object[][] indexMetas = new Object[][]{
                new Object[]{"PRIMARY", "id", false, "", 3, 1, "A", 34},
        };
        Object[][] beforeReturnValue = new Object[][]{};
        StatementProxy beforeMockStatementProxy = mockStatementProxy(returnValueColumnLabels, beforeReturnValue, columnMetas, indexMetas);
        String sql = "update t1 inner join t2 on t1.id = t2.id set t1.name = 'WILL',t2.name = 'WILL'";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        MySQLUpdateRecognizer recognizer = new MySQLUpdateRecognizer(sql, asts.get(0));
        UpdateExecutor mySQLUpdateJoinExecutor = new MySQLUpdateJoinExecutor(beforeMockStatementProxy, (statement, args) -> {
            return null;
        }, recognizer);
        TableRecords beforeImage = mySQLUpdateJoinExecutor.beforeImage();
        Object[][] afterReturnValue = new Object[][]{};
        StatementProxy afterMockStatementProxy = mockStatementProxy(returnValueColumnLabels, afterReturnValue, columnMetas, indexMetas);
        mySQLUpdateJoinExecutor.statementProxy = afterMockStatementProxy;
        TableRecords afterImage = mySQLUpdateJoinExecutor.afterImage(beforeImage);
        Assertions.assertDoesNotThrow(()->mySQLUpdateJoinExecutor.prepareUndoLog(beforeImage, afterImage));
    }

    private StatementProxy mockStatementProxy(List<String> returnValueColumnLabels, Object[][] returnValue, Object[][] columnMetas, Object[][] indexMetas) {
        MockDriver mockDriver = new MockDriver(returnValueColumnLabels, returnValue, columnMetas, indexMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy dataSourceProxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        try {
            Field field = dataSourceProxy.getClass().getDeclaredField("dbType");
            field.setAccessible(true);
            field.set(dataSourceProxy, "mysql");
            ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, dataSource.getConnection().getConnection());
            MockStatementBase mockStatement = new MockStatement(dataSource.getConnection().getConnection());
            return new StatementProxy(connectionProxy, mockStatement);
        } catch (Exception e) {
            throw new RuntimeException("init failed");
        }
    }
}
