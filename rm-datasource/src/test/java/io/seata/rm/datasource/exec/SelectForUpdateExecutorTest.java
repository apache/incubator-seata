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

import java.lang.reflect.Field;
import java.sql.Types;
import java.util.List;
import com.alibaba.druid.mock.MockStatement;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;

import com.google.common.collect.Lists;
import io.seata.core.context.RootContext;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.mock.MockConnectionProxy;
import io.seata.rm.datasource.mock.MockDriver;
import io.seata.rm.datasource.mock.MockLockConflictConnectionProxy;
import io.seata.rm.datasource.sql.druid.MySQLSelectForUpdateRecognizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author will
 */
public class SelectForUpdateExecutorTest {

    private static SelectForUpdateExecutor selectForUpdateExecutor;

    private static ConnectionProxy connectionProxy;

    private static StatementProxy statementProxy;

    @BeforeAll
    public static void init() {
        List<String> returnValueColumnLabels = Lists.newArrayList("id", "name");
        Object[][] returnValue = new Object[][] {
            new Object[] {1, "Tom"},
            new Object[] {2, "Jack"},
        };
        Object[][] columnMetas = new Object[][] {
            new Object[] {"", "", "table_select_for_update_executor_test", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
            new Object[] {"", "", "table_select_for_update_executor_test", "name", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
        };
        Object[][] indexMetas = new Object[][] {
            new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},
        };

        MockDriver mockDriver = new MockDriver(returnValueColumnLabels, returnValue, columnMetas, indexMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy dataSourceProxy = new DataSourceProxy(dataSource);
        try {
            Field field = dataSourceProxy.getClass().getDeclaredField("dbType");
            field.setAccessible(true);
            field.set(dataSourceProxy, "mysql");
            connectionProxy = new MockConnectionProxy(dataSourceProxy, dataSource.getConnection().getConnection());
            connectionProxy.bind("xid");
            MockStatement mockStatement = new MockStatement(dataSource.getConnection().getConnection());
            statementProxy = new StatementProxy(connectionProxy, mockStatement);
        } catch (Exception e) {
            throw new RuntimeException("init failed");
        }

        String sql = "select * from table_select_for_update_executor_test where id = 1";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        MySQLSelectForUpdateRecognizer recognizer = new MySQLSelectForUpdateRecognizer(sql, asts.get(0));
        selectForUpdateExecutor = new SelectForUpdateExecutor(statementProxy, (statement, args) -> {
            return null;
        }, recognizer);
    }

    @Test
    public void testDoExecute() throws Throwable {
        Assertions.assertThrows(RuntimeException.class, () -> {
            selectForUpdateExecutor.doExecute(null);
        });
        RootContext.bind("xid");
        Assertions.assertDoesNotThrow(() -> {
            selectForUpdateExecutor.doExecute(null);
        });
        RootContext.unbind();

        RootContext.bindGlobalLockFlag();
        Assertions.assertDoesNotThrow(() -> {
            selectForUpdateExecutor.doExecute(null);
        });
        RootContext.unbindGlobalLockFlag();

        connectionProxy = new MockLockConflictConnectionProxy(connectionProxy.getDataSourceProxy(), connectionProxy.getTargetConnection());
        statementProxy = new StatementProxy(connectionProxy, statementProxy.getTargetStatement());
        statementProxy.getTargetStatement().getConnection().setAutoCommit(false);
        String sql = "select * from dual";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        MySQLSelectForUpdateRecognizer recognizer = new MySQLSelectForUpdateRecognizer(sql, asts.get(0));
        selectForUpdateExecutor = new SelectForUpdateExecutor(statementProxy, (statement, args) -> null, recognizer);

        RootContext.bind("xid");
        Assertions.assertThrows(LockWaitTimeoutException.class, () -> {
            selectForUpdateExecutor.doExecute(null);
        });
        RootContext.unbind();
    }
}
