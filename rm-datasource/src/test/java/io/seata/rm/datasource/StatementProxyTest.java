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
package io.seata.rm.datasource;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import com.alibaba.druid.mock.MockResultSet;
import com.alibaba.druid.mock.MockStatement;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.util.jdbc.ResultSetMetaDataBase;

import com.google.common.collect.Lists;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.rm.datasource.mock.MockConnection;
import io.seata.rm.datasource.mock.MockDriver;

import io.seata.sqlparser.SQLRecognizerFactory;
import io.seata.sqlparser.SqlParserType;
import io.seata.sqlparser.druid.DruidDelegatingSQLRecognizerFactory;
import io.seata.sqlparser.druid.SQLOperateRecognizerHolder;
import io.seata.sqlparser.druid.SQLOperateRecognizerHolderFactory;
import io.seata.sqlparser.util.JdbcConstants;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * @author will
 */
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
public class StatementProxyTest {

    private static List<String> returnValueColumnLabels = Lists.newArrayList("id", "name");

    private static Object[][] returnValue = new Object[][] {
        new Object[] {1, "Tom"},
        new Object[] {2, "Jack"},
    };

    private static Object[][] columnMetas = new Object[][] {
        new Object[] {"", "", "table_statement_proxy", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64,
            1, "NO", "YES"},
        new Object[] {"", "", "table_statement_proxy", "name", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64,
            2, "YES", "NO"},
    };

    private static Object[][] indexMetas = new Object[][] {
        new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},
    };

    private static StatementProxy statementProxy;

    @BeforeAll
    public static void init() throws SQLException {
        MockDriver mockDriver = new MockDriver(returnValueColumnLabels, returnValue, columnMetas, indexMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy dataSourceProxy = new DataSourceProxy(dataSource);

        ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy,
            dataSource.getConnection().getConnection());

        Statement statement = mockDriver.createMockStatement((MockConnection)connectionProxy.getTargetConnection());

        MockResultSet mockResultSet = new MockResultSet(statement);
        ((ResultSetMetaDataBase)mockResultSet.getMetaData()).getColumns().add(new ResultSetMetaDataBase.ColumnMetaData());
        ((MockStatement) statement).setGeneratedKeys(mockResultSet);

        statementProxy = new StatementProxy(connectionProxy, statement);
        EnhancedServiceLoader.load(SQLOperateRecognizerHolder.class, JdbcConstants.MYSQL,
            SQLOperateRecognizerHolderFactory.class.getClassLoader());
        DruidDelegatingSQLRecognizerFactory recognizerFactory = (DruidDelegatingSQLRecognizerFactory) EnhancedServiceLoader
            .load(SQLRecognizerFactory.class, SqlParserType.SQL_PARSER_TYPE_DRUID);
    }

    @AfterEach
    public void clear() throws SQLException {
        statementProxy.clearBatch();
    }

    @Test
    public void testStatementProxy() {
        Assertions.assertNotNull(statementProxy);
    }

    @Test
    public void testGetConnectionProxy() {
        Assertions.assertNotNull(statementProxy.getConnectionProxy());
    }

    @Test
    public void testExecute() throws SQLException {
        String sql = "select * from table_statment_proxy";
        Assertions.assertNotNull(statementProxy.executeQuery(sql));
        Assertions.assertDoesNotThrow(() -> statementProxy.executeUpdate(sql));
        Assertions.assertDoesNotThrow(() -> statementProxy.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS));
        Assertions.assertDoesNotThrow(() -> statementProxy.executeUpdate(sql, new int[]{1}));
        Assertions.assertDoesNotThrow(() -> statementProxy.executeUpdate(sql, new String[]{"id"}));
        Assertions.assertDoesNotThrow(() -> statementProxy.execute(sql));
        Assertions.assertDoesNotThrow(() -> statementProxy.execute(sql, Statement.RETURN_GENERATED_KEYS));
        Assertions.assertDoesNotThrow(() -> statementProxy.execute(sql, new int[]{1}));
        Assertions.assertDoesNotThrow(() -> statementProxy.execute(sql, new String[]{"id"}));
        Assertions.assertDoesNotThrow(() -> statementProxy.executeBatch());
        Assertions.assertDoesNotThrow(() -> statementProxy.clearBatch());
    }

    @Test
    public void testGetTargetStatement() {
        Assertions.assertNotNull(statementProxy.getTargetStatement());
    }

    @Test
    public void testGetTargetSQL() throws SQLException{
        String qrySql = "select * from table_statment_proxy";
        Assertions.assertNotNull(statementProxy.executeQuery(qrySql));
        Assertions.assertNotNull(statementProxy.getTargetSQL());
        Assertions.assertDoesNotThrow(() -> statementProxy.clearBatch());
        Assertions.assertNull(statementProxy.getTargetSQL());

        String insertSql = "insert into t(id) values (?)";
        Assertions.assertDoesNotThrow(() -> statementProxy.executeUpdate(insertSql, new int[]{1}));
        Assertions.assertNotNull(statementProxy.getTargetSQL());
        Assertions.assertDoesNotThrow(() -> statementProxy.clearBatch());
        Assertions.assertNull(statementProxy.getTargetSQL());

        String updateSql = "update t set t.x=? where t.id=?";
        Assertions.assertDoesNotThrow(() -> statementProxy.executeUpdate(updateSql, new int[]{1}));
        Assertions.assertNotNull(statementProxy.getTargetSQL());
        Assertions.assertDoesNotThrow(() -> statementProxy.clearBatch());
        Assertions.assertNull(statementProxy.getTargetSQL());

        statementProxy.addBatch("insert into t(id) values (1)");
        statementProxy.addBatch("insert into t(id) values (2)");
        Assertions.assertNotNull(statementProxy.getTargetSQL());
        Assertions.assertDoesNotThrow(() -> statementProxy.clearBatch());
        Assertions.assertNull(statementProxy.getTargetSQL());

        statementProxy.addBatch("update t set t.x = x+1 where t.id = 1");
        statementProxy.addBatch("update t set t.x = x+1 where t.id = 2");
        Assertions.assertNotNull(statementProxy.getTargetSQL());
        Assertions.assertDoesNotThrow(() -> statementProxy.clearBatch());
        Assertions.assertNull(statementProxy.getTargetSQL());

        statementProxy.addBatch("delete from t where t.id = 1");
        statementProxy.addBatch("delete from t where t.id = 2");
        Assertions.assertNotNull(statementProxy.getTargetSQL());
        Assertions.assertDoesNotThrow(() -> statementProxy.clearBatch());
    }

    @Test
    public void testMaxFieldSize() throws SQLException {
        statementProxy.setMaxFieldSize(1);
        Assertions.assertEquals(1, statementProxy.getMaxFieldSize());
    }

    @Test
    public void testMaxRows() throws SQLException {
        statementProxy.setMaxRows(1);
        Assertions.assertEquals(1, statementProxy.getMaxRows());
    }

    @Test
    public void testEscapeProcessing() throws SQLException {
        Assertions.assertDoesNotThrow(() -> statementProxy.setEscapeProcessing(true));
    }

    @Test
    public void testQueryTimeout() throws SQLException {
        statementProxy.setQueryTimeout(1);
        Assertions.assertEquals(1, statementProxy.getQueryTimeout());
    }

    @Test
    public void testCancel() {
        Assertions.assertDoesNotThrow(() -> statementProxy.cancel());
    }

    @Test
    public void testWarnings() throws SQLException {
        Assertions.assertNull(statementProxy.getWarnings());
        statementProxy.clearWarnings();
        Assertions.assertNull(statementProxy.getWarnings());
    }

    @Test
    public void testCursorName() {
        Assertions.assertDoesNotThrow(() -> statementProxy.setCursorName("x"));
    }

    @Test
    public void testResultSet() throws SQLException {
        Assertions.assertNotNull(statementProxy.getUpdateCount());
    }

    @Test
    public void testUpdateCount() throws SQLException {
        Assertions.assertEquals(0, statementProxy.getUpdateCount());
    }

    @Test
    public void testMoreResults() throws SQLException {
        Assertions.assertFalse(statementProxy.getMoreResults());
    }

    @Test
    public void testFetchDirection() throws SQLException {
        statementProxy.setFetchDirection(1);
        Assertions.assertEquals(1, statementProxy.getFetchDirection());
    }

    @Test
    public void testFetchSize() throws SQLException {
        statementProxy.setFetchSize(1);
        Assertions.assertEquals(1, statementProxy.getFetchSize());
    }

    @Test
    public void testResultSetConcurrency() throws SQLException {
        Assertions.assertEquals(0, statementProxy.getResultSetConcurrency());
    }

    @Test
    public void testResultSetType() throws SQLException {
        Assertions.assertEquals(0, statementProxy.getResultSetType());
    }

    @Test
    public void testBatch() throws SQLException {
        statementProxy.addBatch("update t set x = 'x' where id = 1");
        Assertions.assertDoesNotThrow(() -> statementProxy.executeBatch());
        Assertions.assertDoesNotThrow(() -> statementProxy.clearBatch());
    }

    @Test
    public void testGetMoreResults() throws SQLException {
        Assertions.assertFalse(statementProxy.getMoreResults(1));
    }

    @Test
    public void testGetGeneratedKeys() {
        Assertions.assertDoesNotThrow(() -> statementProxy.getGeneratedKeys());
    }

    @Test
    public void testGetResultSetHoldability() {
        Assertions.assertDoesNotThrow(() -> statementProxy.getResultSetHoldability());
    }

    @Test
    public void testIsClosed() {
        Assertions.assertDoesNotThrow(() -> statementProxy.isClosed());
    }

    @Test
    public void testPoolable() throws SQLException {
        statementProxy.setPoolable(true);
        Assertions.assertTrue(statementProxy.isPoolable());
    }

    @Test
    public void testCloseOnCompletion() {
        Assertions.assertThrows(SQLFeatureNotSupportedException.class, () -> statementProxy.closeOnCompletion());
        Assertions.assertThrows(SQLFeatureNotSupportedException.class, () -> statementProxy.isCloseOnCompletion());
    }

    @Test
    public void testWrap() throws SQLException {
        Assertions.assertDoesNotThrow(() -> statementProxy.unwrap(String.class));
        Assertions.assertFalse(statementProxy.isWrapperFor(String.class));
    }

}
