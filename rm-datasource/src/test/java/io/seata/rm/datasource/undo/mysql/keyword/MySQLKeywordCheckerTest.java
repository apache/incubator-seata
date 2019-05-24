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
package io.seata.rm.datasource.undo.mysql.keyword;

import java.sql.Types;

import com.alibaba.druid.util.JdbcConstants;
import io.seata.rm.datasource.sql.SQLType;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.KeyType;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.KeywordChecker;
import io.seata.rm.datasource.undo.KeywordCheckerFactory;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.rm.datasource.undo.mysql.MySQLUndoDeleteExecutor;
import io.seata.rm.datasource.undo.mysql.MySQLUndoInsertExecutor;
import io.seata.rm.datasource.undo.mysql.MySQLUndoUpdateExecutor;

import io.seata.rm.datasource.undo.UndoExecutorTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type My sql keyword checker test.
 *
 * @author Wu
 * @date 2019 /3/5 The type MySQL keyword checker test.
 */
public class MySQLKeywordCheckerTest {

    /**
     * Test check
     */
    @Test
    public void testCheck() {
        KeywordChecker keywordChecker = KeywordCheckerFactory.getKeywordChecker(JdbcConstants.MYSQL);
        Assertions.assertTrue(keywordChecker.check("desc"));

    }

    /**
     * Test check and replace
     */
    @Test
    public void testCheckAndReplace() {
        KeywordChecker keywordChecker = KeywordCheckerFactory.getKeywordChecker(JdbcConstants.MYSQL);
        Assertions.assertEquals("`desc`", keywordChecker.checkAndReplace("desc"));

    }

    /**
     * Test keyword check with UPDATE case
     */
    @Test
    public void testUpdateKeywordCheck() {
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setTableName("lock");
        sqlUndoLog.setSqlType(SQLType.UPDATE);

        TableRecords beforeImage = new TableRecords(new UndoExecutorTest.MockTableMeta("product", "key"));

        Row beforeRow = new Row();

        Field pkField = new Field();
        pkField.setKeyType(KeyType.PrimaryKey);
        pkField.setName("key");
        pkField.setType(Types.INTEGER);
        pkField.setValue(213);
        beforeRow.add(pkField);

        Field name = new Field();
        name.setName("desc");
        name.setType(Types.VARCHAR);
        name.setValue("SEATA");
        beforeRow.add(name);

        Field since = new Field();
        since.setName("since");
        since.setType(Types.VARCHAR);
        since.setValue("2014");
        beforeRow.add(since);

        beforeImage.add(beforeRow);

        TableRecords afterImage = new TableRecords(new UndoExecutorTest.MockTableMeta("product", "key"));

        Row afterRow = new Row();

        Field pkField1 = new Field();
        pkField1.setKeyType(KeyType.PrimaryKey);
        pkField1.setName("key");
        pkField1.setType(Types.INTEGER);
        pkField1.setValue(214);
        afterRow.add(pkField1);

        Field name1 = new Field();
        name1.setName("desc");
        name1.setType(Types.VARCHAR);
        name1.setValue("GTS");
        afterRow.add(name1);

        Field since1 = new Field();
        since1.setName("since");
        since1.setType(Types.VARCHAR);
        since1.setValue("2016");
        afterRow.add(since1);

        afterImage.add(afterRow);

        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);

        MySQLUndoUpdateExecutorExtension mySQLUndoUpdateExecutor = new MySQLUndoUpdateExecutorExtension(sqlUndoLog);

        Assertions.assertEquals("UPDATE `lock` SET `desc` = ?, since = ? WHERE `key` = ?",
            mySQLUndoUpdateExecutor.getSql());

    }

    private static class MySQLUndoUpdateExecutorExtension extends MySQLUndoUpdateExecutor {
        /**
         * Instantiates a new My sql undo update executor.
         *
         * @param sqlUndoLog the sql undo log
         */
        public MySQLUndoUpdateExecutorExtension(SQLUndoLog sqlUndoLog) {
            super(sqlUndoLog);
        }

        /**
         * Gets sql.
         *
         * @return the sql
         */
        public String getSql() {
            return super.buildUndoSQL();
        }
    }

    /**
     * Test keyword check with INSERT case
     */
    @Test
    public void testInsertKeywordCheck() {
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setTableName("lock");
        sqlUndoLog.setSqlType(SQLType.INSERT);

        TableRecords beforeImage = TableRecords.empty(new UndoExecutorTest.MockTableMeta("product", "key"));

        TableRecords afterImage = new TableRecords(new UndoExecutorTest.MockTableMeta("product", "key"));

        Row afterRow1 = new Row();

        Field pkField = new Field();
        pkField.setKeyType(KeyType.PrimaryKey);
        pkField.setName("key");
        pkField.setType(Types.INTEGER);
        pkField.setValue(213);
        afterRow1.add(pkField);

        Field name = new Field();
        name.setName("desc");
        name.setType(Types.VARCHAR);
        name.setValue("SEATA");
        afterRow1.add(name);

        Field since = new Field();
        since.setName("since");
        since.setType(Types.VARCHAR);
        since.setValue("2014");
        afterRow1.add(since);

        Row afterRow = new Row();

        Field pkField1 = new Field();
        pkField1.setKeyType(KeyType.PrimaryKey);
        pkField1.setName("key");
        pkField1.setType(Types.INTEGER);
        pkField1.setValue(214);
        afterRow.add(pkField1);

        Field name1 = new Field();
        name1.setName("desc");
        name1.setType(Types.VARCHAR);
        name1.setValue("GTS");
        afterRow.add(name1);

        Field since1 = new Field();
        since1.setName("since");
        since1.setType(Types.VARCHAR);
        since1.setValue("2016");
        afterRow.add(since1);

        afterImage.add(afterRow1);
        afterImage.add(afterRow);

        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);

        MySQLUndoInsertExecutorExtension mySQLUndoInsertExecutor = new MySQLUndoInsertExecutorExtension(sqlUndoLog);

        Assertions.assertEquals("DELETE FROM `lock` WHERE `key` = ?", mySQLUndoInsertExecutor.getSql());

    }

    private static class MySQLUndoInsertExecutorExtension extends MySQLUndoInsertExecutor {
        /**
         * Instantiates a new My sql undo insert executor.
         *
         * @param sqlUndoLog the sql undo log
         */
        public MySQLUndoInsertExecutorExtension(SQLUndoLog sqlUndoLog) {
            super(sqlUndoLog);
        }

        /**
         * Gets sql.
         *
         * @return the sql
         */
        public String getSql() {
            return super.buildUndoSQL();
        }
    }

    /**
     * Test keyword check with DELETE case
     */
    @Test
    public void testDeleteKeywordCheck() {
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setTableName("lock");
        sqlUndoLog.setSqlType(SQLType.DELETE);

        TableRecords afterImage = TableRecords.empty(new UndoExecutorTest.MockTableMeta("product", "id"));

        TableRecords beforeImage = new TableRecords(new UndoExecutorTest.MockTableMeta("product", "id"));

        Row afterRow1 = new Row();

        Field pkField = new Field();
        pkField.setKeyType(KeyType.PrimaryKey);
        pkField.setName("key");
        pkField.setType(Types.INTEGER);
        pkField.setValue(213);
        afterRow1.add(pkField);

        Field name = new Field();
        name.setName("desc");
        name.setType(Types.VARCHAR);
        name.setValue("SEATA");
        afterRow1.add(name);

        Field since = new Field();
        since.setName("since");
        since.setType(Types.VARCHAR);
        since.setValue("2014");
        afterRow1.add(since);

        Row afterRow = new Row();

        Field pkField1 = new Field();
        pkField1.setKeyType(KeyType.PrimaryKey);
        pkField1.setName("key");
        pkField1.setType(Types.INTEGER);
        pkField1.setValue(214);
        afterRow.add(pkField1);

        Field name1 = new Field();
        name1.setName("desc");
        name1.setType(Types.VARCHAR);
        name1.setValue("GTS");
        afterRow.add(name1);

        Field since1 = new Field();
        since1.setName("since");
        since1.setType(Types.VARCHAR);
        since1.setValue("2016");
        afterRow.add(since1);

        beforeImage.add(afterRow1);
        beforeImage.add(afterRow);

        sqlUndoLog.setAfterImage(afterImage);
        sqlUndoLog.setBeforeImage(beforeImage);

        MySQLUndoDeleteExecutorExtension mySQLUndoDeleteExecutor = new MySQLUndoDeleteExecutorExtension(sqlUndoLog);

        Assertions.assertEquals("INSERT INTO `lock` (`desc`, since, `key`) VALUES (?, ?, ?)",
            mySQLUndoDeleteExecutor.getSql());

    }

    private static class MySQLUndoDeleteExecutorExtension extends MySQLUndoDeleteExecutor {
        /**
         * Instantiates a new My sql undo delete executor.
         *
         * @param sqlUndoLog the sql undo log
         */
        public MySQLUndoDeleteExecutorExtension(SQLUndoLog sqlUndoLog) {
            super(sqlUndoLog);
        }

        /**
         * Gets sql.
         *
         * @return the sql
         */
        public String getSql() {
            return super.buildUndoSQL();
        }
    }

}
