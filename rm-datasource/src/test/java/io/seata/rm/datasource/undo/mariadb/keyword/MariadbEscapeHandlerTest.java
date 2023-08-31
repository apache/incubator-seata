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
package io.seata.rm.datasource.undo.mariadb.keyword;

import java.sql.Types;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.KeyType;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.rm.datasource.undo.UndoExecutorTest;
import io.seata.rm.datasource.undo.mariadb.MariadbUndoDeleteExecutor;
import io.seata.rm.datasource.undo.mariadb.MariadbUndoInsertExecutor;
import io.seata.rm.datasource.undo.mariadb.MariadbUndoUpdateExecutor;
import io.seata.sqlparser.EscapeHandler;
import io.seata.sqlparser.EscapeHandlerFactory;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Mariadb keyword checker test.
 *
 * @author funkye
 */
public class MariadbEscapeHandlerTest {
    /**
     * Test check
     */
    @Test
    public void testCheck() {
        EscapeHandler escapeHandler = EscapeHandlerFactory.getEscapeHandler(JdbcConstants.MARIADB);
        Assertions.assertTrue(escapeHandler.checkIfKeyWords("desc"));
    }

    /**
     * Test keyword check with UPDATE case
     */
    @Test
    public void testUpdateKeywordCheck() {
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setTableName("`lock`");
        sqlUndoLog.setSqlType(SQLType.UPDATE);

        TableRecords beforeImage = new TableRecords(new UndoExecutorTest.MockTableMeta("product", "key"));

        Row beforeRow = new Row();

        Field pkField = new Field();
        pkField.setKeyType(KeyType.PRIMARY_KEY);
        pkField.setName("`key`");
        pkField.setType(Types.INTEGER);
        pkField.setValue(213);
        beforeRow.add(pkField);

        Field name = new Field();
        name.setName("`desc`");
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
        pkField1.setKeyType(KeyType.PRIMARY_KEY);
        pkField1.setName("`key`");
        pkField1.setType(Types.INTEGER);
        pkField1.setValue(214);
        afterRow.add(pkField1);

        Field name1 = new Field();
        name1.setName("`desc`");
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

        MariadbEscapeHandlerTest.MariadbUndoUpdateExecutorExtension mariadbUndoUpdateExecutorExtension =
            new MariadbEscapeHandlerTest.MariadbUndoUpdateExecutorExtension(sqlUndoLog);

        Assertions.assertEquals("UPDATE `lock` SET `desc` = ?, since = ? WHERE `key` = ?",
            mariadbUndoUpdateExecutorExtension.getSql().trim());

    }

    /**
     * Test keyword check with INSERT case
     */
    @Test
    public void testInsertKeywordCheck() {
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setTableName("`lock`");
        sqlUndoLog.setSqlType(SQLType.INSERT);

        TableRecords beforeImage = TableRecords.empty(new UndoExecutorTest.MockTableMeta("product", "key"));

        TableRecords afterImage = new TableRecords(new UndoExecutorTest.MockTableMeta("product", "key"));

        Row afterRow1 = new Row();

        Field pkField = new Field();
        pkField.setKeyType(KeyType.PRIMARY_KEY);
        pkField.setName("`key`");
        pkField.setType(Types.INTEGER);
        pkField.setValue(213);
        afterRow1.add(pkField);

        Field name = new Field();
        name.setName("`desc`");
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
        pkField1.setKeyType(KeyType.PRIMARY_KEY);
        pkField1.setName("`key`");
        pkField1.setType(Types.INTEGER);
        pkField1.setValue(214);
        afterRow.add(pkField1);

        Field name1 = new Field();
        name1.setName("`desc`");
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

        MariadbEscapeHandlerTest.MariadbUndoInsertExecutorExtension mariadbUndoInsertExecutorExtension =
            new MariadbEscapeHandlerTest.MariadbUndoInsertExecutorExtension(sqlUndoLog);

        Assertions.assertEquals("DELETE FROM `lock` WHERE `key` = ?",
            mariadbUndoInsertExecutorExtension.getSql().trim());

    }

    /**
     * Test keyword check with DELETE case
     */
    @Test
    public void testDeleteKeywordCheck() {
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setTableName("`lock`");
        sqlUndoLog.setSqlType(SQLType.DELETE);

        TableRecords afterImage = TableRecords.empty(new UndoExecutorTest.MockTableMeta("product", "key"));

        TableRecords beforeImage = new TableRecords(new UndoExecutorTest.MockTableMeta("product", "key"));

        Row afterRow1 = new Row();

        Field pkField = new Field();
        pkField.setKeyType(KeyType.PRIMARY_KEY);
        pkField.setName("`key`");
        pkField.setType(Types.INTEGER);
        pkField.setValue(213);
        afterRow1.add(pkField);

        Field name = new Field();
        name.setName("`desc`");
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
        pkField1.setKeyType(KeyType.PRIMARY_KEY);
        pkField1.setName("`key`");
        pkField1.setType(Types.INTEGER);
        pkField1.setValue(214);
        afterRow.add(pkField1);

        Field name1 = new Field();
        name1.setName("`desc`");
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

        MariadbEscapeHandlerTest.MariadbUndoDeleteExecutorExtension mariadbUndoDeleteExecutorExtension =
            new MariadbEscapeHandlerTest.MariadbUndoDeleteExecutorExtension(sqlUndoLog);

        Assertions.assertEquals("INSERT INTO `lock` (`desc`, since, `key`) VALUES (?, ?, ?)",
            mariadbUndoDeleteExecutorExtension.getSql());

    }

    private static class MariadbUndoUpdateExecutorExtension extends MariadbUndoUpdateExecutor {
        /**
         * Instantiates a new Mariadb undo update executor.
         *
         * @param sqlUndoLog the sql undo log
         */
        public MariadbUndoUpdateExecutorExtension(SQLUndoLog sqlUndoLog) {
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

    private static class MariadbUndoInsertExecutorExtension extends MariadbUndoInsertExecutor {
        /**
         * Instantiates a new Mariadb undo insert executor.
         *
         * @param sqlUndoLog the sql undo log
         */
        public MariadbUndoInsertExecutorExtension(SQLUndoLog sqlUndoLog) {
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

    private static class MariadbUndoDeleteExecutorExtension extends MariadbUndoDeleteExecutor {

        /**
         * Instantiates a new My sql undo insert executor.
         *
         * @param sqlUndoLog the sql undo log
         */
        public MariadbUndoDeleteExecutorExtension(SQLUndoLog sqlUndoLog) {
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
