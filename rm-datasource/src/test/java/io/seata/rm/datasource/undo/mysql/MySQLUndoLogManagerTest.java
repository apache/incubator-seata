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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import com.alibaba.druid.pool.DruidDataSource;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.seata.rm.datasource.ConnectionContext;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.mock.MockDriver;
import io.seata.rm.datasource.undo.parser.JacksonUndoLogParser;
import org.junit.jupiter.api.Assertions;
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

    @BeforeEach
    public void init() throws SQLException {
        MockDriver mockDriver = new MockDriver(returnValueColumnLabels, returnValue, columnMetas, indexMetas);
        dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        dataSourceProxy = new DataSourceProxy(dataSource);
        connectionProxy = new ConnectionProxy(dataSourceProxy, dataSource.getConnection().getConnection());
        undoLogManager = new MySQLUndoLogManager();
    }

    @Test
    public void testGetDbType() {
        Assertions.assertEquals("mysql", undoLogManager.getDbType());
    }

    @Test
    public void testDeleteUndoLogByLogCreated() throws SQLException {
        Assertions.assertEquals(0, undoLogManager.deleteUndoLogByLogCreated(new Date(), 3000, dataSource.getConnection()));
        Assertions.assertThrows(SQLException.class, () -> {
            undoLogManager.deleteUndoLogByLogCreated(new Date(), 3000, connectionProxy);
        });
    }

    @Test
    public void testInsertUndoLog() throws SQLException {
        Assertions.assertDoesNotThrow(() -> {
            undoLogManager.insertUndoLogWithGlobalFinished("xid", 1L, new JacksonUndoLogParser(),
                dataSource.getConnection());
        });

        Assertions.assertDoesNotThrow(() -> {
            undoLogManager.insertUndoLogWithNormal("xid", 1L, "", new byte[]{}, dataSource.getConnection());
        });

        Assertions.assertThrows(SQLException.class, () -> {
            undoLogManager.deleteUndoLogByLogCreated(new Date(), 3000, connectionProxy);
        });

    }

    @Test
    public void testSerializer() {
        MySQLUndoLogManager.setCurrentSerializer("jackson");
        Assertions.assertEquals("jackson", MySQLUndoLogManager.getCurrentSerializer());
        MySQLUndoLogManager.removeCurrentSerializer();
        Assertions.assertEquals(null, MySQLUndoLogManager.getCurrentSerializer());
    }

    @Test
    public void testDeleteUndoLog() {
        Assertions.assertDoesNotThrow(() -> {
            undoLogManager.deleteUndoLog("xid", 1L, dataSource.getConnection());
        });

        Assertions.assertThrows(SQLException.class, () -> {
            undoLogManager.deleteUndoLog("xid", 1L, connectionProxy);
        });
    }

    @Test
    public void testBatchDeleteUndoLog() {
        Assertions.assertDoesNotThrow(() -> {
            undoLogManager.batchDeleteUndoLog(Sets.newHashSet("xid"), Sets.newHashSet(1L), dataSource.getConnection());
        });

        Assertions.assertThrows(SQLException.class, () -> {
            undoLogManager.batchDeleteUndoLog(Sets.newHashSet("xid"), Sets.newHashSet(1L), connectionProxy);
        });
    }

    @Test
    public void testFlushUndoLogs() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        connectionProxy.bind("xid");
        ConnectionContext context = connectionProxy.getContext();
        Method method = context.getClass().getDeclaredMethod("setBranchId", Long.class);
        method.setAccessible(true);
        method.invoke(context, 1L);


        Assertions.assertDoesNotThrow(() -> {
            undoLogManager.flushUndoLogs(connectionProxy);
        });
    }

    @Test
    public void testUndo() throws SQLException {
        Assertions.assertDoesNotThrow(() -> {
            undoLogManager.undo(dataSourceProxy, "xid", 1L);
        });
    }
}
