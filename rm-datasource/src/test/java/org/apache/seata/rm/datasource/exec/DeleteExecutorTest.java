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
import org.apache.seata.rm.datasource.exec.DeleteExecutor;
import org.apache.seata.rm.datasource.mock.MockDriver;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.sqlparser.druid.mysql.MySQLDeleteRecognizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


public class DeleteExecutorTest {

    private static DeleteExecutor deleteExecutor;

    private static StatementProxy statementProxy;

    @BeforeAll
    public static void init() {
        List<String> returnValueColumnLabels = Lists.newArrayList("id", "name");
        Object[][] returnValue = new Object[][] {
            new Object[] {1, "Tom"},
            new Object[] {2, "Jack"},
        };
        Object[][] columnMetas = new Object[][] {
            new Object[] {"", "", "table_delete_executor_test", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
            new Object[] {"", "", "table_delete_executor_test", "name", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
        };
        Object[][] indexMetas = new Object[][] {
            new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},
        };

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
            statementProxy = new StatementProxy(connectionProxy, mockStatement);
        } catch (Exception e) {
            throw new RuntimeException("init failed");
        }
        String sql = "delete from table_delete_executor_test where id = 1";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        MySQLDeleteRecognizer recognizer = new MySQLDeleteRecognizer(sql, asts.get(0));
        deleteExecutor = new DeleteExecutor(statementProxy, (statement, args) -> {
            return null;
        }, recognizer);
    }

    @Test
    public void testBeforeAndAfterImage() throws SQLException {
        String sql = "delete from table_delete_executor_test";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        MySQLDeleteRecognizer recognizer = new MySQLDeleteRecognizer(sql, asts.get(0));
        deleteExecutor = new DeleteExecutor(statementProxy, (statement, args) -> null, recognizer);

        TableRecords beforeImage = deleteExecutor.beforeImage();
        TableRecords afterImage = deleteExecutor.afterImage(beforeImage);
        Assertions.assertNotNull(beforeImage);
        Assertions.assertNotNull(afterImage);
    }

    @Test
    public void testBeforeAndAfterImageWithTableAlias() throws SQLException {
        String sql = "delete from table_delete_executor_test t where t.id = 1";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        MySQLDeleteRecognizer recognizer = new MySQLDeleteRecognizer(sql, asts.get(0));
        deleteExecutor = new DeleteExecutor(statementProxy, (statement, args) -> null, recognizer);

        TableRecords beforeImage = deleteExecutor.beforeImage();
        TableRecords afterImage = deleteExecutor.afterImage(beforeImage);
        Assertions.assertNotNull(beforeImage);
        Assertions.assertNotNull(afterImage);
    }

    @Test
    public void testBeforeAndAfterImageWithTableSchema() throws SQLException {
        String sql = "delete from seata.table_delete_executor_test where id = 1";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        MySQLDeleteRecognizer recognizer = new MySQLDeleteRecognizer(sql, asts.get(0));
        deleteExecutor = new DeleteExecutor(statementProxy, (statement, args) -> null, recognizer);

        TableRecords beforeImage = deleteExecutor.beforeImage();
        TableRecords afterImage = deleteExecutor.afterImage(beforeImage);
        Assertions.assertNotNull(beforeImage);
        Assertions.assertNotNull(afterImage);
    }

    @Test
    public void testBeforeAndAfterImageWithTableSchemaAndTableAlias() throws SQLException {
        String sql = "delete from seata.table_delete_executor_test t where t.id = 1";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        MySQLDeleteRecognizer recognizer = new MySQLDeleteRecognizer(sql, asts.get(0));
        deleteExecutor = new DeleteExecutor(statementProxy, (statement, args) -> null, recognizer);

        TableRecords beforeImage = deleteExecutor.beforeImage();
        TableRecords afterImage = deleteExecutor.afterImage(beforeImage);
        Assertions.assertNotNull(beforeImage);
        Assertions.assertNotNull(afterImage);
    }

    @Test
    public void testBeforeAndAfterImageWithTableSchemaQuote() throws SQLException {
        String sql = "delete from `seata`.table_delete_executor_test where id = 1";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        MySQLDeleteRecognizer recognizer = new MySQLDeleteRecognizer(sql, asts.get(0));
        deleteExecutor = new DeleteExecutor(statementProxy, (statement, args) -> null, recognizer);

        TableRecords beforeImage = deleteExecutor.beforeImage();
        TableRecords afterImage = deleteExecutor.afterImage(beforeImage);
        Assertions.assertNotNull(beforeImage);
        Assertions.assertNotNull(afterImage);
    }

    @Test
    public void testBeforeAndAfterImageWithTableSchemaAndTableNameQuote() throws SQLException {
        String sql = "delete from seata.`table_delete_executor_test` where id = 1";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        MySQLDeleteRecognizer recognizer = new MySQLDeleteRecognizer(sql, asts.get(0));
        deleteExecutor = new DeleteExecutor(statementProxy, (statement, args) -> null, recognizer);

        TableRecords beforeImage = deleteExecutor.beforeImage();
        TableRecords afterImage = deleteExecutor.afterImage(beforeImage);
        Assertions.assertNotNull(beforeImage);
        Assertions.assertNotNull(afterImage);
    }

    @Test
    public void testBeforeAndAfterImageWithTableSchemaQuoteAndTableNameQuote() throws SQLException {
        String sql = "delete from `seata`.`table_delete_executor_test` where id = 1";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        MySQLDeleteRecognizer recognizer = new MySQLDeleteRecognizer(sql, asts.get(0));
        deleteExecutor = new DeleteExecutor(statementProxy, (statement, args) -> null, recognizer);

        TableRecords beforeImage = deleteExecutor.beforeImage();
        TableRecords afterImage = deleteExecutor.afterImage(beforeImage);
        Assertions.assertNotNull(beforeImage);
        Assertions.assertNotNull(afterImage);
    }

    @Test
    public void testBeforeAndAfterImageWithColumnQuote() throws SQLException {
        String sql = "delete from table_delete_executor_test where `id` = 1";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        MySQLDeleteRecognizer recognizer = new MySQLDeleteRecognizer(sql, asts.get(0));
        deleteExecutor = new DeleteExecutor(statementProxy, (statement, args) -> null, recognizer);

        TableRecords beforeImage = deleteExecutor.beforeImage();
        TableRecords afterImage = deleteExecutor.afterImage(beforeImage);
        Assertions.assertNotNull(beforeImage);
        Assertions.assertNotNull(afterImage);
    }

    @Test
    public void testBeforeAndAfterImageWithUpperColumn() throws SQLException {
        String sql = "delete from table_delete_executor_test where ID = 1";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        MySQLDeleteRecognizer recognizer = new MySQLDeleteRecognizer(sql, asts.get(0));
        deleteExecutor = new DeleteExecutor(statementProxy, (statement, args) -> null, recognizer);

        TableRecords beforeImage = deleteExecutor.beforeImage();
        TableRecords afterImage = deleteExecutor.afterImage(beforeImage);
        Assertions.assertNotNull(beforeImage);
        Assertions.assertNotNull(afterImage);
    }

    @Test
    public void testBeforeAndAfterImageWithTableAliasAndUpperColumn() throws SQLException {
        String sql = "delete from table_delete_executor_test t where t.ID = 1";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        MySQLDeleteRecognizer recognizer = new MySQLDeleteRecognizer(sql, asts.get(0));
        deleteExecutor = new DeleteExecutor(statementProxy, (statement, args) -> null, recognizer);

        TableRecords beforeImage = deleteExecutor.beforeImage();
        TableRecords afterImage = deleteExecutor.afterImage(beforeImage);
        Assertions.assertNotNull(beforeImage);
        Assertions.assertNotNull(afterImage);
    }

    @Test
    public void testBeforeAndAfterImageWithKeyword() throws SQLException {
        String sql = "delete from table_delete_executor_test where `or` = 1";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        MySQLDeleteRecognizer recognizer = new MySQLDeleteRecognizer(sql, asts.get(0));
        deleteExecutor = new DeleteExecutor(statementProxy, (statement, args) -> null, recognizer);

        TableRecords beforeImage = deleteExecutor.beforeImage();
        TableRecords afterImage = deleteExecutor.afterImage(beforeImage);
        Assertions.assertNotNull(beforeImage);
        Assertions.assertNotNull(afterImage);
    }

}
