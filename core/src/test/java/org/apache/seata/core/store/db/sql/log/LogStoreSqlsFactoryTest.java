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
package org.apache.seata.core.store.db.sql.log;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class LogStoreSqlsFactoryTest {

    private static LogStoreSqls mysqlLog = LogStoreSqlsFactory.getLogStoreSqls("mysql");

    private static LogStoreSqls oracleLog = LogStoreSqlsFactory.getLogStoreSqls("oracle");

    private static LogStoreSqls pgLog = LogStoreSqlsFactory.getLogStoreSqls("postgresql");

    private static LogStoreSqls h2Log = LogStoreSqlsFactory.getLogStoreSqls("h2");

    private static LogStoreSqls oceanbase = LogStoreSqlsFactory.getLogStoreSqls("oceanbase");

    private static LogStoreSqls dmLog = LogStoreSqlsFactory.getLogStoreSqls("dm");

    private static LogStoreSqls oscarLog = LogStoreSqlsFactory.getLogStoreSqls("oscar");

    private static String globalTable = "global_table";

    private static String branchTable = "branch_table";

    @Test
    public void mysqlLogTest() {

        String sql = mysqlLog.getInsertGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getUpdateGlobalTransactionStatusSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getDeleteGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getQueryGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getQueryGlobalTransactionSQLByTransactionId(globalTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getQueryGlobalTransactionSQLByStatus(globalTable, "1");
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getQueryGlobalTransactionForRecoverySQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getInsertBranchTransactionSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getUpdateBranchTransactionStatusSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getDeleteBranchTransactionByBranchIdSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getDeleteBranchTransactionByXId(branchTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getQueryBranchTransaction(branchTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getQueryBranchTransaction(branchTable, "1");
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getQueryGlobalMax(globalTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getQueryBranchMax(branchTable);
        Assertions.assertNotNull(sql);
    }

    @Test
    public void oracleLogTest() {

        String sql = oracleLog.getInsertGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getUpdateGlobalTransactionStatusSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getDeleteGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getQueryGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getQueryGlobalTransactionSQLByTransactionId(globalTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getQueryGlobalTransactionSQLByStatus(globalTable, "1");
        Assertions.assertNotNull(sql);
        sql = oracleLog.getQueryGlobalTransactionForRecoverySQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getInsertBranchTransactionSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getUpdateBranchTransactionStatusSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getDeleteBranchTransactionByBranchIdSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getDeleteBranchTransactionByXId(branchTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getQueryBranchTransaction(branchTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getQueryBranchTransaction(branchTable, "1");
        Assertions.assertNotNull(sql);
        sql = oracleLog.getQueryGlobalMax(globalTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getQueryBranchMax(branchTable);
        Assertions.assertNotNull(sql);
    }

    @Test
    public void pgLogTest() {

        String sql = pgLog.getInsertGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getUpdateGlobalTransactionStatusSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getDeleteGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getQueryGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getQueryGlobalTransactionSQLByTransactionId(globalTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getQueryGlobalTransactionSQLByStatus(globalTable, "1");
        Assertions.assertNotNull(sql);
        sql = pgLog.getQueryGlobalTransactionForRecoverySQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getInsertBranchTransactionSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getUpdateBranchTransactionStatusSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getDeleteBranchTransactionByBranchIdSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getDeleteBranchTransactionByXId(branchTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getQueryBranchTransaction(branchTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getQueryBranchTransaction(branchTable, "1");
        Assertions.assertNotNull(sql);
        sql = pgLog.getQueryGlobalMax(globalTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getQueryBranchMax(branchTable);
        Assertions.assertNotNull(sql);
    }

    @Test
    public void h2LogTest() {

        String sql = h2Log.getInsertGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getUpdateGlobalTransactionStatusSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getDeleteGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getQueryGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getQueryGlobalTransactionSQLByTransactionId(globalTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getQueryGlobalTransactionSQLByStatus(globalTable, "1");
        Assertions.assertNotNull(sql);
        sql = h2Log.getQueryGlobalTransactionForRecoverySQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getInsertBranchTransactionSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getUpdateBranchTransactionStatusSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getDeleteBranchTransactionByBranchIdSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getDeleteBranchTransactionByXId(branchTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getQueryBranchTransaction(branchTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getQueryBranchTransaction(branchTable, "1");
        Assertions.assertNotNull(sql);
        sql = h2Log.getQueryGlobalMax(globalTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getQueryBranchMax(branchTable);
        Assertions.assertNotNull(sql);
    }

    @Test
    public void oceanbaseLogTest() {

        String sql = oceanbase.getInsertGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getUpdateGlobalTransactionStatusSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getDeleteGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getQueryGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getQueryGlobalTransactionSQLByTransactionId(globalTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getQueryGlobalTransactionSQLByStatus(globalTable, "1");
        Assertions.assertNotNull(sql);
        sql = oceanbase.getQueryGlobalTransactionForRecoverySQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getInsertBranchTransactionSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getUpdateBranchTransactionStatusSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getDeleteBranchTransactionByBranchIdSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getDeleteBranchTransactionByXId(branchTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getQueryBranchTransaction(branchTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getQueryBranchTransaction(branchTable, "1");
        Assertions.assertNotNull(sql);
        sql = oceanbase.getQueryGlobalMax(globalTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getQueryBranchMax(branchTable);
        Assertions.assertNotNull(sql);
    }

    @Test
    public void dmLogTest() {
        String sql = dmLog.getInsertGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = dmLog.getUpdateGlobalTransactionStatusSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = dmLog.getDeleteGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = dmLog.getQueryGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = dmLog.getQueryGlobalTransactionSQLByTransactionId(globalTable);
        Assertions.assertNotNull(sql);
        sql = dmLog.getQueryGlobalTransactionSQLByStatus(globalTable, "1");
        Assertions.assertNotNull(sql);
        sql = dmLog.getQueryGlobalTransactionForRecoverySQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = dmLog.getInsertBranchTransactionSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = dmLog.getUpdateBranchTransactionStatusSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = dmLog.getDeleteBranchTransactionByBranchIdSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = dmLog.getDeleteBranchTransactionByXId(branchTable);
        Assertions.assertNotNull(sql);
        sql = dmLog.getQueryBranchTransaction(branchTable);
        Assertions.assertNotNull(sql);
        sql = dmLog.getQueryBranchTransaction(branchTable, "1");
        Assertions.assertNotNull(sql);
        sql = dmLog.getQueryGlobalMax(globalTable);
        Assertions.assertNotNull(sql);
        sql = dmLog.getQueryBranchMax(branchTable);
        Assertions.assertNotNull(sql);
    }

    @Test
    public void oscarLogTest() {
        String sql = oscarLog.getInsertGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oscarLog.getUpdateGlobalTransactionStatusSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oscarLog.getDeleteGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oscarLog.getQueryGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oscarLog.getQueryGlobalTransactionSQLByTransactionId(globalTable);
        Assertions.assertNotNull(sql);
        sql = oscarLog.getQueryGlobalTransactionSQLByStatus(globalTable, "1");
        Assertions.assertNotNull(sql);
        sql = oscarLog.getQueryGlobalTransactionForRecoverySQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oscarLog.getInsertBranchTransactionSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = oscarLog.getUpdateBranchTransactionStatusSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = oscarLog.getDeleteBranchTransactionByBranchIdSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = oscarLog.getDeleteBranchTransactionByXId(branchTable);
        Assertions.assertNotNull(sql);
        sql = oscarLog.getQueryBranchTransaction(branchTable);
        Assertions.assertNotNull(sql);
        sql = oscarLog.getQueryBranchTransaction(branchTable, "1");
        Assertions.assertNotNull(sql);
        sql = oscarLog.getQueryGlobalMax(globalTable);
        Assertions.assertNotNull(sql);
        sql = oscarLog.getQueryBranchMax(branchTable);
        Assertions.assertNotNull(sql);
    }

    @ParameterizedTest
    @ValueSource(strings = {"mysql", "postgresql", "h2", "dm", "mariadb", "oceanbase", "polardb-x"})
    public void limitDialectsKeysetPaginationSqlTest(String dbType) {
        LogStoreSqls logStoreSqls = LogStoreSqlsFactory.getLogStoreSqls(dbType);
        String orderedSql = logStoreSqls.getQueryGlobalTransactionSQLByStatusOrdered(globalTable, "?,?");
        assertCommonOrderedSql(orderedSql);
        Assertions.assertTrue(orderedSql.endsWith("order by gmt_modified asc, xid asc limit ?"), orderedSql);
        Assertions.assertEquals(3, countQuestionMarks(orderedSql), orderedSql);

        String cursorSql = logStoreSqls.getQueryGlobalTransactionSQLByStatusAfterCursor(globalTable, "?,?");
        assertCommonCursorSql(cursorSql);
        Assertions.assertTrue(cursorSql.endsWith("order by gmt_modified asc, xid asc limit ?"), cursorSql);
        Assertions.assertEquals(6, countQuestionMarks(cursorSql), cursorSql);
    }

    @ParameterizedTest
    @ValueSource(strings = {"oracle", "kingbase", "oscar"})
    public void oracleDialectsKeysetPaginationSqlTest(String dbType) {
        LogStoreSqls logStoreSqls = LogStoreSqlsFactory.getLogStoreSqls(dbType);
        String orderedSql = logStoreSqls.getQueryGlobalTransactionSQLByStatusOrdered(globalTable, "?,?");
        assertCommonOrderedSql(orderedSql);
        Assertions.assertTrue(orderedSql.contains("ROWNUM <= ?"), orderedSql);
        Assertions.assertTrue(
                orderedSql.indexOf("order by gmt_modified asc, xid asc") < orderedSql.indexOf("ROWNUM <= ?"),
                orderedSql);
        Assertions.assertEquals(3, countQuestionMarks(orderedSql), orderedSql);

        String cursorSql = logStoreSqls.getQueryGlobalTransactionSQLByStatusAfterCursor(globalTable, "?,?");
        assertCommonCursorSql(cursorSql);
        Assertions.assertTrue(cursorSql.contains("ROWNUM <= ?"), cursorSql);
        Assertions.assertTrue(
                cursorSql.indexOf("order by gmt_modified asc, xid asc") < cursorSql.indexOf("ROWNUM <= ?"), cursorSql);
        Assertions.assertEquals(6, countQuestionMarks(cursorSql), cursorSql);
    }

    @Test
    public void sqlServerDialectKeysetPaginationSqlTest() {
        LogStoreSqls logStoreSqls = LogStoreSqlsFactory.getLogStoreSqls("sqlserver");
        String orderedSql = logStoreSqls.getQueryGlobalTransactionSQLByStatusOrdered(globalTable, "?,?");
        assertCommonOrderedSql(orderedSql);
        Assertions.assertTrue(orderedSql.startsWith("select top (?) "), orderedSql);
        Assertions.assertTrue(orderedSql.indexOf("top (?)") < orderedSql.indexOf("status in (?,?)"), orderedSql);
        Assertions.assertTrue(orderedSql.endsWith("order by gmt_modified asc, xid asc"), orderedSql);
        Assertions.assertEquals(3, countQuestionMarks(orderedSql), orderedSql);

        String cursorSql = logStoreSqls.getQueryGlobalTransactionSQLByStatusAfterCursor(globalTable, "?,?");
        assertCommonCursorSql(cursorSql);
        Assertions.assertTrue(cursorSql.startsWith("select top (?) "), cursorSql);
        Assertions.assertTrue(cursorSql.indexOf("top (?)") < cursorSql.indexOf("status in (?,?)"), cursorSql);
        Assertions.assertTrue(cursorSql.endsWith("order by gmt_modified asc, xid asc"), cursorSql);
        Assertions.assertEquals(6, countQuestionMarks(cursorSql), cursorSql);
    }

    @Test
    public void customLogStoreSqlsDefaultMethodsThrowUnsupportedOperationException() {
        LogStoreSqls minimalCustomSpi = new AbstractLogStoreSqls() {
            @Override
            public String getInsertGlobalTransactionSQL(String globalTable) {
                return "";
            }

            @Override
            public String getUpdateGlobalTransactionStatusSQL(String globalTable) {
                return "";
            }

            @Override
            public String getUpdateGlobalTransactionStatusByStatusSQL(String globalTable) {
                return "";
            }

            @Override
            public String getQueryGlobalTransactionSQLByStatus(String globalTable, String paramsPlaceHolder) {
                return "";
            }

            @Override
            public String getQueryGlobalTransactionForRecoverySQL(String globalTable) {
                return "";
            }

            @Override
            public String getInsertBranchTransactionSQL(String branchTable) {
                return "";
            }

            @Override
            public String getUpdateBranchTransactionStatusSQL(String branchTable) {
                return "";
            }

            @Override
            public String getUpdateBranchTransactionStatusAppDataSQL(String branchTable) {
                return "";
            }
        };

        UnsupportedOperationException orderedEx = Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> minimalCustomSpi.getQueryGlobalTransactionSQLByStatusOrdered(globalTable, "?"));
        Assertions.assertTrue(
                orderedEx.getMessage().contains("Keyset pagination SQL is not supported by "), orderedEx.getMessage());
        Assertions.assertTrue(
                orderedEx.getMessage().contains(minimalCustomSpi.getClass().getName()), orderedEx.getMessage());

        UnsupportedOperationException cursorEx = Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> minimalCustomSpi.getQueryGlobalTransactionSQLByStatusAfterCursor(globalTable, "?"));
        Assertions.assertTrue(
                cursorEx.getMessage().contains("Keyset pagination SQL is not supported by "), cursorEx.getMessage());
        Assertions.assertTrue(
                cursorEx.getMessage().contains(minimalCustomSpi.getClass().getName()), cursorEx.getMessage());
    }

    private static void assertCommonOrderedSql(String sql) {
        Assertions.assertNotNull(sql);
        Assertions.assertTrue(sql.contains("from " + globalTable), sql);
        Assertions.assertTrue(sql.contains("status in (?,?)"), sql);
        Assertions.assertTrue(sql.contains("gmt_modified is not null"), sql);
        Assertions.assertTrue(sql.contains("order by gmt_modified asc, xid asc"), sql);
        Assertions.assertTrue(sql.indexOf("status in (?,?)") < sql.indexOf("gmt_modified is not null"), sql);
        Assertions.assertTrue(
                sql.indexOf("gmt_modified is not null") < sql.indexOf("order by gmt_modified asc, xid asc"), sql);
        Assertions.assertFalse(sql.contains("gmt_modified > ?"), sql);
    }

    private static void assertCommonCursorSql(String sql) {
        Assertions.assertNotNull(sql);
        Assertions.assertTrue(sql.contains("from " + globalTable), sql);
        Assertions.assertTrue(sql.contains("status in (?,?)"), sql);
        Assertions.assertTrue(sql.contains("gmt_modified is not null"), sql);
        Assertions.assertTrue(sql.contains("(gmt_modified > ? or (gmt_modified = ? and xid > ?))"), sql);
        Assertions.assertFalse(sql.contains("(gmt_modified, xid)"), sql);
        Assertions.assertTrue(sql.contains("order by gmt_modified asc, xid asc"), sql);
        Assertions.assertTrue(sql.indexOf("status in (?,?)") < sql.indexOf("gmt_modified is not null"), sql);
        Assertions.assertTrue(
                sql.indexOf("gmt_modified is not null")
                        < sql.indexOf("(gmt_modified > ? or (gmt_modified = ? and xid > ?))"),
                sql);
        Assertions.assertTrue(
                sql.indexOf("(gmt_modified > ? or (gmt_modified = ? and xid > ?))")
                        < sql.indexOf("order by gmt_modified asc, xid asc"),
                sql);
    }

    private static int countQuestionMarks(String sql) {
        int count = 0;
        for (int i = 0; i < sql.length(); i++) {
            if (sql.charAt(i) == '?') {
                count++;
            }
        }
        return count;
    }
}
