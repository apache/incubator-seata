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
package org.apache.seata.rm.datasource.undo.mysql.keyword;

import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.KeyType;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.rm.datasource.undo.UndoExecutorTest;
import org.apache.seata.rm.datasource.undo.mysql.MySQLUndoDeleteExecutor;
import org.apache.seata.rm.datasource.undo.mysql.MySQLUndoInsertExecutor;
import org.apache.seata.rm.datasource.undo.mysql.MySQLUndoUpdateExecutor;
import org.apache.seata.sqlparser.EscapeHandler;
import org.apache.seata.sqlparser.EscapeHandlerFactory;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.util.ColumnUtils;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.Types;
import java.util.stream.Stream;

/**
 * The type My sql keyword checker test.
 *
 */
public class MySQLEscapeHandlerTest {

    /**
     * Test check
     */
    @Test
    public void testCheck() {
        EscapeHandler escapeHandler = EscapeHandlerFactory.getEscapeHandler(JdbcConstants.MYSQL);
        Assertions.assertTrue(escapeHandler.checkIfKeyWords("desc"));
    }

    @ParameterizedTest
    @MethodSource("columnNamesToEscape")
    public void testColumnNameEscaping(String columnName, String expected) {
        String escaped = ColumnUtils.addEscape(columnName, JdbcConstants.MYSQL);
        Assertions.assertEquals(expected, escaped);
        Assertions.assertEquals(expected, ColumnUtils.addEscape(escaped, JdbcConstants.MYSQL));
    }

    private static Stream<Arguments> columnNamesToEscape() {
        return Stream.of(
                Arguments.of("current_ quantity", "`current_ quantity`"),
                Arguments.of("current_\tquantity", "`current_\tquantity`"),
                Arguments.of("current_\nquantity", "`current_\nquantity`"),
                Arguments.of(" quantity", "` quantity`"),
                Arguments.of("\tquantity", "`\tquantity`"),
                Arguments.of("`current_ quantity`", "`current_ quantity`"),
                Arguments.of("`current_. quantity`", "`current_. quantity`"),
                Arguments.of("sku.current_ quantity", "`sku`.`current_ quantity`"),
                Arguments.of("`sku`.current_ quantity", "`sku`.`current_ quantity`"),
                Arguments.of("sku.`current_ quantity`", "sku.`current_ quantity`"),
                Arguments.of("`sku`.`current_ quantity`", "`sku`.`current_ quantity`"),
                Arguments.of("quantity", "quantity"),
                Arguments.of("desc", "`desc`"),
                Arguments.of("`desc`", "`desc`"),
                Arguments.of("", ""),
                Arguments.of(" ", " "),
                Arguments.of(null, null));
    }

    @Test
    public void testUpdateWithWhitespaceColumnNames() {
        SQLUndoLog sqlUndoLog = whitespaceColumnUndoLog(SQLType.UPDATE);
        Assertions.assertEquals(
                "UPDATE sku SET `current_ quantity` = ? WHERE `sku id` = ?",
                new MySQLUndoUpdateExecutorExtension(sqlUndoLog).getSql().trim());
    }

    @Test
    public void testInsertWithWhitespacePrimaryKey() {
        SQLUndoLog sqlUndoLog = whitespaceColumnUndoLog(SQLType.INSERT);
        Assertions.assertEquals(
                "DELETE FROM sku WHERE `sku id` = ?",
                new MySQLUndoInsertExecutorExtension(sqlUndoLog).getSql().trim());
    }

    @Test
    public void testDeleteWithWhitespaceColumnNames() {
        SQLUndoLog sqlUndoLog = whitespaceColumnUndoLog(SQLType.DELETE);
        Assertions.assertEquals(
                "INSERT INTO sku (`current_ quantity`, `sku id`) VALUES (?, ?)",
                new MySQLUndoDeleteExecutorExtension(sqlUndoLog).getSql());
    }

    private static SQLUndoLog whitespaceColumnUndoLog(SQLType sqlType) {
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setTableName("sku");
        sqlUndoLog.setSqlType(sqlType);
        sqlUndoLog.setBeforeImage(
                sqlType == SQLType.INSERT
                        ? TableRecords.empty(new UndoExecutorTest.MockTableMeta("sku", "sku id"))
                        : whitespaceColumnImage(7240));
        sqlUndoLog.setAfterImage(
                sqlType == SQLType.DELETE
                        ? TableRecords.empty(new UndoExecutorTest.MockTableMeta("sku", "sku id"))
                        : whitespaceColumnImage(7241));
        return sqlUndoLog;
    }

    private static TableRecords whitespaceColumnImage(int quantity) {
        TableRecords image = new TableRecords(new UndoExecutorTest.MockTableMeta("sku", "sku id"));
        Row row = new Row();
        Field pkField = new Field("sku id", Types.INTEGER, 1068);
        pkField.setKeyType(KeyType.PRIMARY_KEY);
        row.add(pkField);
        row.add(new Field("current_ quantity", Types.INTEGER, quantity));
        image.add(row);
        return image;
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

        MySQLUndoUpdateExecutorExtension mySQLUndoUpdateExecutor = new MySQLUndoUpdateExecutorExtension(sqlUndoLog);

        Assertions.assertEquals(
                "UPDATE `lock` SET `desc` = ?, since = ? WHERE `key` = ?",
                mySQLUndoUpdateExecutor.getSql().trim());
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

        MySQLUndoInsertExecutorExtension mySQLUndoInsertExecutor = new MySQLUndoInsertExecutorExtension(sqlUndoLog);

        Assertions.assertEquals(
                "DELETE FROM `lock` WHERE `key` = ?",
                mySQLUndoInsertExecutor.getSql().trim());
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

        MySQLUndoDeleteExecutorExtension mySQLUndoDeleteExecutor = new MySQLUndoDeleteExecutorExtension(sqlUndoLog);

        Assertions.assertEquals(
                "INSERT INTO `lock` (`desc`, since, `key`) VALUES (?, ?, ?)", mySQLUndoDeleteExecutor.getSql());
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
