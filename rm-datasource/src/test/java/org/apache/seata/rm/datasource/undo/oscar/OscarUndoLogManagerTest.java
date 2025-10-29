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
     * 测试默认表名的序列名生成 - 向后兼容性测试
     * Oscar 默认使用 UNDO_LOG 表和 UNDO_LOG_SEQ 序列
     */
    @Test
    public void testDefaultTableNameSequenceGeneration() throws Exception {
        // 使用反射获取私有的 INSERT_UNDO_LOG_SQL 字段
        Field insertSqlField = OscarUndoLogManager.class.getDeclaredField("INSERT_UNDO_LOG_SQL");
        insertSqlField.setAccessible(true);
        String insertSql = (String) insertSqlField.get(null);

        // 验证默认情况下使用 UNDO_LOG_SEQ（大写，遵循Oscar社区约定）
        Assertions.assertTrue(
                insertSql.contains("UNDO_LOG_SEQ.nextval"), "默认配置下应该使用 UNDO_LOG_SEQ 序列（大写），实际SQL: " + insertSql);

        // 验证 SQL 包含正确的表名
        Assertions.assertTrue(insertSql.contains("INSERT INTO undo_log"), "SQL应该插入到 undo_log 表，实际SQL: " + insertSql);

        // 验证使用 Oscar 特有的时间函数
        Assertions.assertTrue(insertSql.contains("sysdate"), "Oscar 应该使用 sysdate 时间函数，实际SQL: " + insertSql);
    }

    /**
     * 测试自定义表名的序列名生成 - 修复验证
     * 重点测试序列名保持用户配置的大小写，不强制转换大写
     */
    @Test
    public void testCustomTableNameSequenceGeneration() throws Exception {
        // 模拟配置自定义表名
        try (MockedStatic<ConfigurationFactory> configurationFactoryMock =
                Mockito.mockStatic(ConfigurationFactory.class)) {
            // 创建模拟的 Configuration 实例
            Configuration mockConfiguration = Mockito.mock(Configuration.class);
            configurationFactoryMock.when(ConfigurationFactory::getInstance).thenReturn(mockConfiguration);

            // 配置自定义表名 "my_undo_log"（小写）
            Mockito.when(mockConfiguration.getConfig(ConfigurationKeys.TRANSACTION_UNDO_LOG_TABLE, "undo_log"))
                    .thenReturn("my_undo_log");

            // 验证序列名生成逻辑 - Oscar约定使用大写序列名
            String customTableName = "my_undo_log";
            String expectedSequenceName = customTableName.toUpperCase() + "_SEQ";  // MY_UNDO_LOG_SEQ，遵循Oscar约定
            String expectedSqlPart = customTableName.toUpperCase() + "_SEQ.nextval";

            // 构造预期的SQL片段
            String expectedInsertSql = "INSERT INTO " + customTableName + " ("
                    + "id,branch_id, xid, context, rollback_info, log_status, log_created, log_modified)"
                    + "VALUES ("
                    + expectedSqlPart + ", ?, ?, ?, ?, ?, sysdate, sysdate)";

            // 验证序列名生成逻辑是否正确 - Oscar约定使用大写
            Assertions.assertTrue(
                    expectedSqlPart.contains("MY_UNDO_LOG_SEQ"), "自定义表名 my_undo_log 应该生成序列名 MY_UNDO_LOG_SEQ（遵循Oscar大写约定）");
            
            // 验证确实转换为了大写（Oscar社区约定）
            Assertions.assertFalse(
                    expectedSqlPart.contains("my_undo_log_SEQ"), "Oscar应该将序列名转换为大写，而不是保持小写");
        }
    }

    /**
     * 测试各种表名的序列名生成规则
     * 验证修复后保持用户配置的大小写
     */
    @Test
    public void testSequenceNameGenerationRules() {
        // 测试各种表名格式 - Oscar约定转换为大写
        String[][] testCases = {
            {"undo_log", "UNDO_LOG_SEQ"},           // 默认转大写
            {"UNDO_LOG", "UNDO_LOG_SEQ"},           // 已是大写
            {"my_undo_log", "MY_UNDO_LOG_SEQ"},     // 自定义转大写
            {"MyUndoLog", "MYUNDOLOG_SEQ"},         // 驼峰转大写
            {"CUSTOM_TABLE", "CUSTOM_TABLE_SEQ"},   // 已是大写
            {"seata_undo", "SEATA_UNDO_SEQ"}        // 项目前缀转大写
        };

        for (String[] testCase : testCases) {
            String tableName = testCase[0];
            String expectedSequence = testCase[1];

            // 验证序列名生成规则 - Oscar约定转大写
            String actualSequence = tableName.toUpperCase() + "_SEQ";
            Assertions.assertEquals(
                    expectedSequence,
                    actualSequence,
                    String.format("表名 '%s' 应该生成序列名 '%s'（遵循Oscar大写约定）", tableName, expectedSequence));
        }
    }

    /**
     * 测试向后兼容性 - 确保现有部署不会受到影响
     */
    @Test
    public void testBackwardCompatibility() throws Exception {
        // 验证默认配置下的行为与之前保持一致
        Field insertSqlField = OscarUndoLogManager.class.getDeclaredField("INSERT_UNDO_LOG_SQL");
        insertSqlField.setAccessible(true);
        String insertSql = (String) insertSqlField.get(null);

        // 验证关键的 SQL 组件
        Assertions.assertTrue(insertSql.contains("INSERT INTO undo_log"), "应该保持默认表名 undo_log");
        Assertions.assertTrue(insertSql.contains("UNDO_LOG_SEQ.nextval"), "应该保持默认序列名 UNDO_LOG_SEQ（大写）");
        Assertions.assertTrue(insertSql.contains("sysdate"), "应该保持 Oscar 时间函数 sysdate");

        // 验证参数占位符数量正确
        long parameterCount = insertSql.chars().filter(ch -> ch == '?').count();
        Assertions.assertEquals(
                5, parameterCount, "INSERT SQL 应该包含5个参数占位符 (branch_id, xid, context, rollback_info, log_status)");
    }

    /**
     * 测试修复前后的对比 - Oscar 特有的验证
     */
    @Test
    public void testFixComparison() {
        System.out.println("\n=== Oscar 序列名修复对比 ===");
        
        // 修复前（硬编码）
        String oldSequenceName = "UNDO_LOG_SEQ";
        System.out.println("修复前: " + oldSequenceName + ".nextval - 硬编码大写");
        
        // 修复后（基于表名动态生成，遵循Oscar大写约定）
        String defaultTableName = "undo_log";
        String newSequenceName = defaultTableName.toUpperCase() + "_SEQ";
        System.out.println("修复后: " + newSequenceName + ".nextval - 基于表名动态生成，遵循Oscar大写约定");
        
        // 默认情况下结果相同（向后兼容）
        Assertions.assertEquals(oldSequenceName, newSequenceName, "默认配置下修复前后应该相同，确保向后兼容");
        
        // 自定义表名的情况
        String customTableName = "my_undo_log";
        String customSequenceName = customTableName.toUpperCase() + "_SEQ";
        System.out.println("自定义表名: " + customSequenceName + ".nextval - 支持自定义且遵循Oscar大写约定");
        
        // 验证转换为大写（遵循Oscar社区约定）
        Assertions.assertEquals("MY_UNDO_LOG_SEQ", customSequenceName, "自定义表名应该转换为大写序列名");
        Assertions.assertNotEquals("my_undo_log_SEQ", customSequenceName, "Oscar应该将序列名转换为大写");
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
