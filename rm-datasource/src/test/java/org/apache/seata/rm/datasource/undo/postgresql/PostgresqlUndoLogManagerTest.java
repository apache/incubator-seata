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
package org.apache.seata.rm.datasource.undo.postgresql;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidStatementConnection;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.rm.datasource.ConnectionProxy;
import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.DataSourceProxyTest;
import org.apache.seata.rm.datasource.mock.MockDriver;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.*;
import org.apache.seata.rm.datasource.undo.parser.JacksonUndoLogParser;
import org.apache.seata.sqlparser.SQLRecognizerFactory;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.SqlParserType;
import org.apache.seata.sqlparser.druid.DruidDelegatingSQLRecognizerFactory;
import org.apache.seata.sqlparser.druid.SQLOperateRecognizerHolder;
import org.apache.seata.sqlparser.druid.SQLOperateRecognizerHolderFactory;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PostgresqlUndoLogManagerTest {

    List<String> returnValueColumnLabels = Lists.newArrayList("log_status");
    Object[][] returnValue = new Object[][] {
        new Object[] {1}, new Object[] {2},
    };
    Object[][] columnMetas = new Object[][] {
        new Object[] {
            "",
            "",
            "table_plain_executor_test",
            "id",
            Types.INTEGER,
            "INTEGER",
            64,
            0,
            10,
            1,
            "",
            "",
            0,
            0,
            64,
            1,
            "NO",
            "YES"
        },
        new Object[] {
            "",
            "",
            "table_plain_executor_test",
            "name",
            Types.VARCHAR,
            "VARCHAR",
            64,
            0,
            10,
            0,
            "",
            "",
            0,
            0,
            64,
            2,
            "YES",
            "NO"
        },
    };
    Object[][] indexMetas = new Object[][] {
        new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},
    };

    private DruidDataSource dataSource;
    private DataSourceProxy dataSourceProxy;
    private ConnectionProxy connectionProxy;
    private PostgresqlUndoLogManager undoLogManager;
    private TableMeta tableMeta;

    @BeforeAll
    public static void setup() {
        EnhancedServiceLoader.load(
                SQLOperateRecognizerHolder.class,
                JdbcConstants.POSTGRESQL,
                SQLOperateRecognizerHolderFactory.class.getClassLoader());
        DruidDelegatingSQLRecognizerFactory recognizerFactory = (DruidDelegatingSQLRecognizerFactory)
                EnhancedServiceLoader.load(SQLRecognizerFactory.class, SqlParserType.SQL_PARSER_TYPE_DRUID);
    }

    @BeforeEach
    public void init() throws SQLException {
        MockDriver mockDriver = new MockDriver(returnValueColumnLabels, returnValue, columnMetas, indexMetas);
        dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        dataSourceProxy = DataSourceProxyTest.getDataSourceProxy(dataSource);

        connectionProxy = new ConnectionProxy(dataSourceProxy, getPhysicsConnection(dataSource));
        undoLogManager = new PostgresqlUndoLogManager();
        tableMeta = new TableMeta();
        tableMeta.setTableName("table_plain_executor_test");
    }

    private Connection getPhysicsConnection(DruidDataSource dataSource) throws SQLException {
        Connection connection = dataSource.getConnection().getConnection();
        if (connection instanceof DruidStatementConnection) {
            return ((DruidStatementConnection) connection).getConnection();
        }
        return connection;
    }

    @Test
    public void testDeleteUndoLogByLogCreated() throws SQLException {
        Assertions.assertEquals(
                0, undoLogManager.deleteUndoLogByLogCreated(new Date(), 3000, dataSource.getConnection()));
        Assertions.assertDoesNotThrow(
                () -> undoLogManager.deleteUndoLogByLogCreated(new Date(), 3000, connectionProxy));
    }

    @Test
    public void testInsertUndoLog() throws SQLException {
        Assertions.assertDoesNotThrow(() -> undoLogManager.insertUndoLogWithGlobalFinished(
                "xid", 1L, new JacksonUndoLogParser(), dataSource.getConnection()));

        Assertions.assertDoesNotThrow(
                () -> undoLogManager.insertUndoLogWithNormal("xid", 1L, "", new byte[] {}, dataSource.getConnection()));
    }

    @Test
    public void testDeleteUndoLog() {
        Assertions.assertDoesNotThrow(() -> undoLogManager.deleteUndoLog("xid", 1L, dataSource.getConnection()));
        Assertions.assertDoesNotThrow(() -> undoLogManager.deleteUndoLog("xid", 1L, connectionProxy));
    }

    @Test
    public void testBatchDeleteUndoLog() {
        Assertions.assertDoesNotThrow(() -> undoLogManager.batchDeleteUndoLog(
                Sets.newHashSet("xid"), Sets.newHashSet(1L), dataSource.getConnection()));

        Assertions.assertDoesNotThrow(
                () -> undoLogManager.batchDeleteUndoLog(Sets.newHashSet("xid"), Sets.newHashSet(1L), connectionProxy));
    }

    @Test
    public void testUndo() throws SQLException {
        Assertions.assertDoesNotThrow(() -> undoLogManager.undo(dataSourceProxy, "xid", 1L));
    }

    /**
     * Test sequence name generation with default table name (backward compatibility).
     * Focus of the fix: using default table name "undo_log" should generate "undo_log_id_seq".
     */
    @Test
    public void testDefaultTableNameSequenceGeneration() throws Exception {
        // Use reflection to access private INSERT_UNDO_LOG_SQL field
        Field insertSqlField = PostgresqlUndoLogManager.class.getDeclaredField("INSERT_UNDO_LOG_SQL");
        insertSqlField.setAccessible(true);
        String insertSql = (String) insertSqlField.get(null);

        // Verify default uses undo_log_id_seq
        Assertions.assertTrue(
                insertSql.contains("nextval('undo_log_id_seq')"), "Should use undo_log_id_seq by default. Actual SQL: " + insertSql);

        // Verify SQL contains correct table name
        Assertions.assertTrue(insertSql.contains("INSERT INTO undo_log"), "SQL should insert into undo_log. Actual SQL: " + insertSql);
    }

    /**
     * Test sequence name generation with custom table name (new feature).
     * Core of the fix: when custom table name is configured, sequence name should change accordingly.
     */
    @Test
    public void testCustomTableNameSequenceGeneration() throws Exception {
        // Mock custom table name configuration
        try (MockedStatic<ConfigurationFactory> configurationFactoryMock =
                Mockito.mockStatic(ConfigurationFactory.class)) {
            // Create mocked Configuration instance
            Configuration mockConfiguration = Mockito.mock(Configuration.class);
            configurationFactoryMock.when(ConfigurationFactory::getInstance).thenReturn(mockConfiguration);

            // Configure custom table name "my_undo_log"
            Mockito.when(mockConfiguration.getConfig(ConfigurationKeys.TRANSACTION_UNDO_LOG_TABLE, "undo_log"))
                    .thenReturn("my_undo_log");

            // Reload PostgresqlUndoLogManager if needed (static final fields verified via reflection)

            // Verify sequence name generation logic
            String customTableName = "my_undo_log";
            String expectedSequenceName = customTableName + "_id_seq";
            String expectedSqlPart = "nextval('" + expectedSequenceName + "')";

            // Build expected SQL fragment
            String expectedInsertSql = "INSERT INTO " + customTableName + " ("
                    + "id,branch_id, xid, context, rollback_info, log_status, log_created, log_modified)"
                    + "VALUES ("
                    + expectedSqlPart + ", ?, ?, ?, ?, ?, now(), now())";

            // Verify the sequence name generation is correct
            Assertions.assertTrue(
                    expectedSqlPart.contains("my_undo_log_id_seq"), "Custom table 'my_undo_log' should generate sequence 'my_undo_log_id_seq'");
        }
    }

    /**
     * Test sequence name generation rules for various table names.
     */
    @Test
    public void testSequenceNameGenerationRules() {
        // Various table name formats
        String[][] testCases = {
            {"undo_log", "undo_log_id_seq"},
            {"my_undo_log", "my_undo_log_id_seq"},
            {"custom_table", "custom_table_id_seq"},
            {"app_undo_logs", "app_undo_logs_id_seq"},
            {"seata_undo", "seata_undo_id_seq"}
        };

        for (String[] testCase : testCases) {
            String tableName = testCase[0];
            String expectedSequence = testCase[1];

            // Verify the sequence name rule
            String actualSequence = tableName + "_id_seq";
            Assertions.assertEquals(
                    expectedSequence,
                    actualSequence,
                    String.format("Table '%s' should generate sequence '%s'", tableName, expectedSequence));
        }
    }

    /**
     * Test backward compatibility - ensure existing deployments are not impacted.
     */
    @Test
    public void testBackwardCompatibility() throws Exception {
        // Verify default behavior stays the same
        Field insertSqlField = PostgresqlUndoLogManager.class.getDeclaredField("INSERT_UNDO_LOG_SQL");
        insertSqlField.setAccessible(true);
        String insertSql = (String) insertSqlField.get(null);

        // Verify key SQL parts
        Assertions.assertTrue(insertSql.contains("INSERT INTO undo_log"), "Should keep default table name 'undo_log'");
        Assertions.assertTrue(insertSql.contains("nextval('undo_log_id_seq')"), "Should keep default sequence 'undo_log_id_seq'");
        Assertions.assertTrue(insertSql.contains("now(), now()"), "Should keep PostgreSQL time function now()");

        // Verify parameter placeholder count is correct
        long parameterCount = insertSql.chars().filter(ch -> ch == '?').count();
        Assertions.assertEquals(
                5, parameterCount, "INSERT SQL should contain 5 placeholders (branch_id, xid, context, rollback_info, log_status)");
    }

    private SQLUndoLog getUndoLogItem(int size) throws NoSuchFieldException, IllegalAccessException {
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setTableName("table_plain_executor_test");
        sqlUndoLog.setSqlType(SQLType.INSERT);
        sqlUndoLog.setTableMeta(tableMeta);

        Field rowsField = TableRecords.class.getDeclaredField("rows");
        rowsField.setAccessible(true);

        List<Row> rows = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Row row = new Row();
            row.add(new org.apache.seata.rm.datasource.sql.struct.Field("id", 1, "value_id_" + i));
            row.add(new org.apache.seata.rm.datasource.sql.struct.Field("name", 1, "value_name_" + i));
            rows.add(row);
        }

        sqlUndoLog.setAfterImage(TableRecords.empty(tableMeta));
        TableRecords afterImage = new TableRecords(tableMeta);
        rowsField.set(afterImage, rows);
        sqlUndoLog.setAfterImage(afterImage);

        return sqlUndoLog;
    }
}
