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
package io.seata.rm.datasource.undo.mysql;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.alibaba.druid.pool.DruidDataSource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.rm.datasource.ConnectionContext;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.mock.MockDriver;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.sqlparser.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.AbstractUndoLogManager;
import io.seata.rm.datasource.undo.BranchUndoLog;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.rm.datasource.undo.UndoLogParser;
import io.seata.rm.datasource.undo.UndoLogParserFactory;
import io.seata.rm.datasource.undo.parser.JacksonUndoLogParser;
import io.seata.sqlparser.SQLRecognizerFactory;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.SqlParserType;
import io.seata.sqlparser.druid.DruidDelegatingSQLRecognizerFactory;
import io.seata.sqlparser.druid.SQLOperateRecognizerHolder;
import io.seata.sqlparser.druid.SQLOperateRecognizerHolderFactory;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author will
 */
public class MySQLUndoLogManagerTest {

    List<String> returnValueColumnLabels = Lists.newArrayList("log_status");
    Object[][] returnValue = new Object[][] {
        new Object[] {1},
        new Object[] {2},
    };
    Object[][] columnMetas = new Object[][] {
        new Object[] {"", "", "table_plain_executor_test", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
        new Object[] {"", "", "table_plain_executor_test", "name", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
    };
    Object[][] indexMetas = new Object[][] {
        new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},
    };

    private DruidDataSource dataSource;

    private DataSourceProxy dataSourceProxy;

    private ConnectionProxy connectionProxy;

    private MySQLUndoLogManager undoLogManager;

    private TableMeta tableMeta;

    @BeforeAll
    public static void setup(){
        EnhancedServiceLoader.load(SQLOperateRecognizerHolder.class, JdbcConstants.MYSQL,
            SQLOperateRecognizerHolderFactory.class.getClassLoader());
        DruidDelegatingSQLRecognizerFactory recognizerFactory = (DruidDelegatingSQLRecognizerFactory) EnhancedServiceLoader
            .load(SQLRecognizerFactory.class, SqlParserType.SQL_PARSER_TYPE_DRUID);
    }

    @BeforeEach
    public void init() throws SQLException {
        MockDriver mockDriver = new MockDriver(returnValueColumnLabels, returnValue, columnMetas, indexMetas);
        dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        dataSourceProxy = new DataSourceProxy(dataSource);
        connectionProxy = new ConnectionProxy(dataSourceProxy, dataSource.getConnection().getConnection());
        undoLogManager = new MySQLUndoLogManager();
        tableMeta = new TableMeta();
        tableMeta.setTableName("table_plain_executor_test");
    }

    @Test
    public void testDeleteUndoLogByLogCreated() throws SQLException {
        Assertions.assertEquals(0, undoLogManager.deleteUndoLogByLogCreated(new Date(), 3000, dataSource.getConnection()));
        Assertions.assertDoesNotThrow(() -> undoLogManager.deleteUndoLogByLogCreated(new Date(), 3000, connectionProxy));
    }

    @Test
    public void testInsertUndoLog() throws SQLException {
        Assertions.assertDoesNotThrow(() -> undoLogManager.insertUndoLogWithGlobalFinished("xid", 1L, new JacksonUndoLogParser(),
            dataSource.getConnection()));

        Assertions.assertDoesNotThrow(() -> undoLogManager.insertUndoLogWithNormal("xid", 1L, "", new byte[]{}, dataSource.getConnection()));

        Assertions.assertDoesNotThrow(() -> undoLogManager.deleteUndoLogByLogCreated(new Date(), 3000, connectionProxy));

    }

    @Test
    public void testSerializer() {
        MySQLUndoLogManager.setCurrentSerializer("jackson");
        Assertions.assertEquals("jackson", MySQLUndoLogManager.getCurrentSerializer());
        MySQLUndoLogManager.removeCurrentSerializer();
        Assertions.assertNull(MySQLUndoLogManager.getCurrentSerializer());
    }

    @Test
    public void testDeleteUndoLog() {
        Assertions.assertDoesNotThrow(() -> undoLogManager.deleteUndoLog("xid", 1L, dataSource.getConnection()));

        Assertions.assertDoesNotThrow(() -> undoLogManager.deleteUndoLog("xid", 1L, connectionProxy));
    }

    @Test
    public void testBatchDeleteUndoLog() {
        Assertions.assertDoesNotThrow(() -> undoLogManager.batchDeleteUndoLog(Sets.newHashSet("xid"), Sets.newHashSet(1L), dataSource.getConnection()));

        Assertions.assertDoesNotThrow(() -> undoLogManager.batchDeleteUndoLog(Sets.newHashSet("xid"), Sets.newHashSet(1L), connectionProxy));
    }

    @Test
    public void testFlushUndoLogs() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        connectionProxy.bind("xid");
        ConnectionContext context = connectionProxy.getContext();
        Method method = context.getClass().getDeclaredMethod("setBranchId", Long.class);
        method.setAccessible(true);
        method.invoke(context, 1L);

        SQLUndoLog undoLogItem = getUndoLogItem(1);
        undoLogItem.setTableName("test");
        Method appendUndoItemMethod = context.getClass().getDeclaredMethod("appendUndoItem", SQLUndoLog.class);
        appendUndoItemMethod.setAccessible(true);
        appendUndoItemMethod.invoke(context, undoLogItem);

        Assertions.assertDoesNotThrow(() -> undoLogManager.flushUndoLogs(connectionProxy));
    }

    @Test
    public void testNeedCompress() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        SQLUndoLog smallUndoItem = getUndoLogItem(1);
        BranchUndoLog smallBranchUndoLog = new BranchUndoLog();
        smallBranchUndoLog.setBranchId(1L);
        smallBranchUndoLog.setXid("test_xid");
        smallBranchUndoLog.setSqlUndoLogs(Collections.singletonList(smallUndoItem));
        UndoLogParser parser = UndoLogParserFactory.getInstance();
        byte[] smallUndoLogContent = parser.encode(smallBranchUndoLog);

        Method method = AbstractUndoLogManager.class.getDeclaredMethod("needCompress", byte[].class);
        method.setAccessible(true);
        Assertions.assertFalse((Boolean) method.invoke(undoLogManager, smallUndoLogContent));

        SQLUndoLog hugeUndoItem = getUndoLogItem(10000);
        BranchUndoLog hugeBranchUndoLog = new BranchUndoLog();
        hugeBranchUndoLog.setBranchId(2L);
        hugeBranchUndoLog.setXid("test_xid1");
        hugeBranchUndoLog.setSqlUndoLogs(Collections.singletonList(hugeUndoItem));
        byte[] hugeUndoLogContent = parser.encode(hugeBranchUndoLog);
        Assertions.assertTrue((Boolean) method.invoke(undoLogManager, hugeUndoLogContent));
    }

    @Test
    public void testUndo() throws SQLException {
        Assertions.assertDoesNotThrow(() -> undoLogManager.undo(dataSourceProxy, "xid", 1L));
    }

    private SQLUndoLog getUndoLogItem(int size) throws NoSuchFieldException, IllegalAccessException {
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setTableName("table_plain_executor_test");
        sqlUndoLog.setSqlType(SQLType.INSERT);
        sqlUndoLog.setTableMeta(tableMeta);

        Field rowsField = TableRecords.class.getDeclaredField("rows");
        rowsField.setAccessible(true);

        List<Row> rows = new ArrayList<>(size);
        for (int i = 0; i < size; i ++) {
            Row row = new Row();
            row.add(new io.seata.rm.datasource.sql.struct.Field("id", 1, "value_id_" + i));
            row.add(new io.seata.rm.datasource.sql.struct.Field("name", 1, "value_name_" + i));
            rows.add(row);
        }

        sqlUndoLog.setAfterImage(TableRecords.empty(tableMeta));
        TableRecords afterImage = new TableRecords(tableMeta);
        rowsField.set(afterImage, rows);
        sqlUndoLog.setAfterImage(afterImage);

        return sqlUndoLog;
    }
}
