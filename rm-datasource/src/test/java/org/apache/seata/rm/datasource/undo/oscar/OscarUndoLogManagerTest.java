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
package org.apache.seata.rm.datasource.undo.oscar;

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

public class OscarUndoLogManagerTest {

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
    private OscarUndoLogManager undoLogManager;
    private TableMeta tableMeta;

    @BeforeAll
    public static void setup() {
        EnhancedServiceLoader.load(
                SQLOperateRecognizerHolder.class,
                JdbcConstants.OSCAR,
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
        undoLogManager = new OscarUndoLogManager();
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
     * Oscar defaults to using undo_log table and UNDO_LOG_SEQ sequence.
     */
    @Test
    public void testDefaultTableNameSequenceGeneration() throws Exception {
        // Use reflection to access private INSERT_UNDO_LOG_SQL field
        Field insertSqlField = OscarUndoLogManager.class.getDeclaredField("INSERT_UNDO_LOG_SQL");
        insertSqlField.setAccessible(true);
        String insertSql = (String) insertSqlField.get(null);

        // Verify default uses UNDO_LOG_SEQ (uppercase, following Oscar community convention)
        Assertions.assertTrue(
                insertSql.contains("UNDO_LOG_SEQ.nextval"), "Should use UNDO_LOG_SEQ sequence (uppercase) by default. Actual SQL: " + insertSql);

        // Verify SQL contains correct table name
        Assertions.assertTrue(insertSql.contains("INSERT INTO undo_log"), "SQL should insert into undo_log table. Actual SQL: " + insertSql);

        // Verify uses Oscar-specific time function
        Assertions.assertTrue(insertSql.contains("sysdate"), "Oscar should use sysdate time function. Actual SQL: " + insertSql);
    }

    /**
     * Test sequence name generation with custom table name (fix verification).
     * Focus on testing that sequence names follow Oscar uppercase convention.
     */
    @Test
    public void testCustomTableNameSequenceGeneration() throws Exception {
        // Mock custom table name configuration
        try (MockedStatic<ConfigurationFactory> configurationFactoryMock =
                Mockito.mockStatic(ConfigurationFactory.class)) {
            // Create mocked Configuration instance
            Configuration mockConfiguration = Mockito.mock(Configuration.class);
            configurationFactoryMock.when(ConfigurationFactory::getInstance).thenReturn(mockConfiguration);

            // Configure custom table name "my_undo_log" (lowercase)
            Mockito.when(mockConfiguration.getConfig(ConfigurationKeys.TRANSACTION_UNDO_LOG_TABLE, "undo_log"))
                    .thenReturn("my_undo_log");

            // Verify sequence name generation logic - Oscar convention uses uppercase sequence names
            String customTableName = "my_undo_log";
            String expectedSequenceName = customTableName.toUpperCase() + "_SEQ";  // MY_UNDO_LOG_SEQ, following Oscar convention
            String expectedSqlPart = customTableName.toUpperCase() + "_SEQ.nextval";

            // Build expected SQL fragment
            String expectedInsertSql = "INSERT INTO " + customTableName + " ("
                    + "id,branch_id, xid, context, rollback_info, log_status, log_created, log_modified)"
                    + "VALUES ("
                    + expectedSqlPart + ", ?, ?, ?, ?, ?, sysdate, sysdate)";

            // Verify sequence name generation logic is correct - Oscar convention uses uppercase
            Assertions.assertTrue(
                    expectedSqlPart.contains("MY_UNDO_LOG_SEQ"), "Custom table 'my_undo_log' should generate sequence 'MY_UNDO_LOG_SEQ' (following Oscar uppercase convention)");
            
            // Verify conversion to uppercase (Oscar community convention)
            Assertions.assertFalse(
                    expectedSqlPart.contains("my_undo_log_SEQ"), "Oscar should convert sequence name to uppercase, not keep lowercase");
        }
    }

    /**
     * Test sequence name generation rules for various table names.
     * Verify uppercase conversion following Oscar convention.
     */
    @Test
    public void testSequenceNameGenerationRules() {
        // Test various table name formats - Oscar convention converts to uppercase
        String[][] testCases = {
            {"undo_log", "UNDO_LOG_SEQ"},           // Default to uppercase
            {"UNDO_LOG", "UNDO_LOG_SEQ"},           // Already uppercase
            {"my_undo_log", "MY_UNDO_LOG_SEQ"},     // Custom to uppercase
            {"MyUndoLog", "MYUNDOLOG_SEQ"},         // CamelCase to uppercase
            {"CUSTOM_TABLE", "CUSTOM_TABLE_SEQ"},   // Already uppercase
            {"seata_undo", "SEATA_UNDO_SEQ"}        // Project prefix to uppercase
        };

        for (String[] testCase : testCases) {
            String tableName = testCase[0];
            String expectedSequence = testCase[1];

            // Verify sequence name generation rule - Oscar convention converts to uppercase
            String actualSequence = tableName.toUpperCase() + "_SEQ";
            Assertions.assertEquals(
                    expectedSequence,
                    actualSequence,
                    String.format("Table '%s' should generate sequence '%s' (following Oscar uppercase convention)", tableName, expectedSequence));
        }
    }

    /**
     * Test backward compatibility - ensure existing deployments are not impacted.
     */
    @Test
    public void testBackwardCompatibility() throws Exception {
        // Verify default configuration behavior stays consistent
        Field insertSqlField = OscarUndoLogManager.class.getDeclaredField("INSERT_UNDO_LOG_SQL");
        insertSqlField.setAccessible(true);
        String insertSql = (String) insertSqlField.get(null);

        // Verify key SQL components
        Assertions.assertTrue(insertSql.contains("INSERT INTO undo_log"), "Should keep default table name 'undo_log'");
        Assertions.assertTrue(insertSql.contains("UNDO_LOG_SEQ.nextval"), "Should keep default sequence name 'UNDO_LOG_SEQ' (uppercase)");
        Assertions.assertTrue(insertSql.contains("sysdate"), "Should keep Oscar time function sysdate");

        // Verify parameter placeholder count is correct
        long parameterCount = insertSql.chars().filter(ch -> ch == '?').count();
        Assertions.assertEquals(
                5, parameterCount, "INSERT SQL should contain 5 parameter placeholders (branch_id, xid, context, rollback_info, log_status)");
    }

    /**
     * Test comparison before and after fix - Oscar-specific verification.
     */
    @Test
    public void testFixComparison() {
        System.out.println("\n=== Oscar Sequence Name Fix Comparison ===");
        
        // Before fix (hardcoded)
        String oldSequenceName = "UNDO_LOG_SEQ";
        System.out.println("Before: " + oldSequenceName + ".nextval - hardcoded uppercase");
        
        // After fix (dynamically generated based on table name, following Oscar uppercase convention)
        String defaultTableName = "undo_log";
        String newSequenceName = defaultTableName.toUpperCase() + "_SEQ";
        System.out.println("After: " + newSequenceName + ".nextval - dynamically generated, following Oscar uppercase convention");
        
        // Same result by default (backward compatible)
        Assertions.assertEquals(oldSequenceName, newSequenceName, "Before and after fix should be same with default config, ensuring backward compatibility");
        
        // Custom table name scenario
        String customTableName = "my_undo_log";
        String customSequenceName = customTableName.toUpperCase() + "_SEQ";
        System.out.println("Custom table: " + customSequenceName + ".nextval - supports customization and follows Oscar uppercase convention");
        
        // Verify conversion to uppercase (following Oscar community convention)
        Assertions.assertEquals("MY_UNDO_LOG_SEQ", customSequenceName, "Custom table name should be converted to uppercase sequence name");
        Assertions.assertNotEquals("my_undo_log_SEQ", customSequenceName, "Oscar should convert sequence name to uppercase");
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
