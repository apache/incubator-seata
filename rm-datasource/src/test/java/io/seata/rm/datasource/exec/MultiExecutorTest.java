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
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.alibaba.druid.mock.MockStatement;
import com.alibaba.druid.mock.MockStatementBase;
import com.alibaba.druid.pool.DruidDataSource;
import com.google.common.collect.Lists;

import io.seata.common.exception.NotSupportYetException;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.mock.MockDriver;
import io.seata.rm.datasource.sql.SQLVisitorFactory;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.util.JdbcConstants;

public class MultiExecutorTest {

    private static MultiExecutor executor;

    private static StatementProxy statementProxy;
    private static MockDriver mockDriver;
    private static ConnectionProxy connectionProxy;

    @BeforeAll
    public static void init() throws Throwable {
        List<String> returnValueColumnLabels = Lists.newArrayList("id", "name");
        Object[][] returnValue = new Object[][]{
                new Object[]{1, "Tom"},
                new Object[]{2, "Jack"},
        };
        Object[][] columnMetas = new Object[][]{
                new Object[]{"", "", "table_multi_executor_test", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
                new Object[]{"", "", "table_multi_executor_test", "name", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
        };
        Object[][] indexMetas = new Object[][]{
                new Object[]{"PRIMARY", "id", false, "", 3, 1, "A", 34},
        };

        mockDriver = new MockDriver(returnValueColumnLabels, returnValue, columnMetas, indexMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy dataSourceProxy = new DataSourceProxy(dataSource);
        try {
            Field field = dataSourceProxy.getClass().getDeclaredField("dbType");
            field.setAccessible(true);
            field.set(dataSourceProxy, "mysql");
            connectionProxy = new ConnectionProxy(dataSourceProxy, dataSource.getConnection().getConnection());
            MockStatementBase mockStatement = new MockStatement(dataSource.getConnection().getConnection());
            statementProxy = new StatementProxy(connectionProxy, mockStatement);
        } catch (Exception e) {
            throw new RuntimeException("init failed");
        }


    }

    @Test
    public void testBeforeImageAndAfterImages() throws SQLException {
        //same table and same type
        String sql = "update table_multi_executor_test set name = 'WILL' where id = 1;" +
                "update table_multi_executor_test set name = 'WILL2' where id = 2";
        List<SQLRecognizer> multi = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        executor = new MultiExecutor(statementProxy, (statement, args) -> {
            return null;
        }, multi);
        TableRecords beforeImage = executor.beforeImage();
        Map multiSqlGroup = executor.getMultiSqlGroup();
        Map beforeImagesMap = executor.getBeforeImagesMap();
        Assertions.assertEquals(multiSqlGroup.size(), 1);
        Assertions.assertEquals(beforeImagesMap.size(), 1);
        TableRecords afterImage = executor.afterImage(beforeImage);
        Assertions.assertEquals(executor.getAfterImagesMap().size(), 1);
        executor.prepareUndoLog(beforeImage, afterImage);
        List<SQLUndoLog> items = connectionProxy.getContext().getUndoItems();
        Assertions.assertTrue(items.stream().allMatch(t -> Objects.equals(t.getSqlType(), SQLType.UPDATE) && Objects.equals(t.getTableName(), "table_multi_executor_test")));
        Assertions.assertEquals(items.size(), 1);
        connectionProxy.getContext().reset();


        //same table delete
        sql = "delete from table_multi_executor_test where id = 2;" +
                "delete from table_multi_executor_test where id = 3";
        multi = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        executor = new MultiExecutor(statementProxy, (statement, args) -> {
            return null;
        }, multi);
        beforeImage = executor.beforeImage();
        multiSqlGroup = executor.getMultiSqlGroup();
        beforeImagesMap = executor.getBeforeImagesMap();
        Assertions.assertEquals(multiSqlGroup.size(), 1);
        Assertions.assertEquals(beforeImagesMap.size(), 1);
        afterImage = executor.afterImage(beforeImage);
        Assertions.assertEquals(executor.getAfterImagesMap().size(), 1);
        executor.prepareUndoLog(beforeImage, afterImage);
        items = connectionProxy.getContext().getUndoItems();
        Set<String> itemSet = items.stream().map(t -> t.getTableName()).collect(Collectors.toSet());
        Assertions.assertTrue(itemSet.contains("table_multi_executor_test"));
        Assertions.assertEquals(items.size(), 1);
        connectionProxy.getContext().reset();


        //multi table update
        sql = "update table_multi_executor_test set name = 'WILL' where id = 1;update table_multi_executor_test2 set name = 'WILL' where id = 1;update table_multi_executor_test2 set name = 'WILL' where id = 3;";
        multi = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        executor = new MultiExecutor(statementProxy, (statement, args) -> {
            return null;
        }, multi);
        beforeImage = executor.beforeImage();
        multiSqlGroup = executor.getMultiSqlGroup();
        beforeImagesMap = executor.getBeforeImagesMap();
        Assertions.assertEquals(multiSqlGroup.size(), 2);
        Assertions.assertEquals(beforeImagesMap.size(), 2);
        afterImage = executor.afterImage(beforeImage);
        Assertions.assertEquals(executor.getAfterImagesMap().size(), 2);
        executor.prepareUndoLog(beforeImage, afterImage);
        items = connectionProxy.getContext().getUndoItems();
        itemSet = items.stream().map(t -> t.getTableName()).collect(Collectors.toSet());
        Assertions.assertTrue(itemSet.contains("table_multi_executor_test"));
        Assertions.assertTrue(itemSet.contains("table_multi_executor_test2"));
        Assertions.assertEquals(items.size(), 2);
        connectionProxy.getContext().reset();


        // multi table delete
        sql = "delete from table_multi_executor_test2 where id = 2;delete from table_multi_executor_test where id = 3;delete from table_multi_executor_test where id = 4;delete from table_multi_executor_test";
        multi = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        executor = new MultiExecutor(statementProxy, (statement, args) -> {
            return null;
        }, multi);
        beforeImage = executor.beforeImage();
        multiSqlGroup = executor.getMultiSqlGroup();
        beforeImagesMap = executor.getBeforeImagesMap();
        Assertions.assertEquals(multiSqlGroup.size(), 2);
        Assertions.assertEquals(beforeImagesMap.size(), 2);
        afterImage = executor.afterImage(beforeImage);
        Assertions.assertEquals(executor.getAfterImagesMap().size(), 2);
        executor.prepareUndoLog(beforeImage, afterImage);
        items = connectionProxy.getContext().getUndoItems();
        itemSet = items.stream().map(t -> t.getTableName()).collect(Collectors.toSet());
        Assertions.assertTrue(itemSet.contains("table_multi_executor_test"));
        Assertions.assertTrue(itemSet.contains("table_multi_executor_test2"));
        Assertions.assertEquals(items.size(), 2);

        // contains limit delete
        sql = "delete from table_multi_executor_test2 where id = 2;delete from table_multi_executor_test2 where id = 2 limit 1;";
        multi = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        executor = new MultiExecutor(statementProxy, (statement, args) -> {
            return null;
        }, multi);
        Assertions.assertThrows(NotSupportYetException.class, executor::beforeImage);

        // contains order by and limit delete
        sql = "delete from table_multi_executor_test2 where id = 2;delete from table_multi_executor_test2 where id = 2 order by id desc limit 1;";
        multi = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        executor = new MultiExecutor(statementProxy, (statement, args) -> {
            return null;
        }, multi);
        Assertions.assertThrows(NotSupportYetException.class, executor::beforeImage);


        //contains order by update
        sql = "update table_multi_executor_test set name = 'WILL' where id = 1;update table_multi_executor_test set name = 'WILL' where id = 1 order by id desc;";
        multi = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        executor = new MultiExecutor(statementProxy, (statement, args) -> {
            return null;
        }, multi);
        Assertions.assertThrows(NotSupportYetException.class, executor::beforeImage);

        //contains order by and limit update
        sql = "update table_multi_executor_test set name = 'WILL' where id = 1;update table_multi_executor_test set name = 'WILL' where id = 1 order by id desc limit 1;";
        multi = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        executor = new MultiExecutor(statementProxy, (statement, args) -> {
            return null;
        }, multi);
        Assertions.assertThrows(NotSupportYetException.class, executor::beforeImage);
    }
}

