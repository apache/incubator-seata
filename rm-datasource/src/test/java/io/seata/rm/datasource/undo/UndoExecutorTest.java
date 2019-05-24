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
package io.seata.rm.datasource.undo;

import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.seata.rm.datasource.sql.SQLType;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.KeyType;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The type Undo executor test.
 */
public class UndoExecutorTest {
    
    /**
     * Test field.
     */
    @Test
    public void testSerializeField() {
        Field f = new Field();
        f.setName("name");
        f.setValue("x");
        f.setType(Types.VARCHAR);
        f.setKeyType(KeyType.PrimaryKey);

        String s = JSON.toJSONString(f, SerializerFeature.WriteDateUseDateFormat);
        Field fd = JSON.parseObject(s, Field.class);

        assertEquals(fd.getKeyType(), KeyType.PrimaryKey);
    }


    /**
     * Test update.
     */
    @Test
    public void testUpdate() throws SQLException {
        String expectedSQL = "UPDATE my_test_table SET name = ?, since = ? WHERE id = ?";

        mockSQLUndoLogAndVerifyExecution(SQLType.UPDATE, expectedSQL, mockedPrepStmt -> {
            try {
                verify(mockedPrepStmt).setObject(1, "SEATA", 12);
                verify(mockedPrepStmt).setObject(2, "2014", 12);
                verify(mockedPrepStmt).setObject(3, 213, 4);
                verify(mockedPrepStmt).executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Test insert.
     */
    @Test
    public void testInsert() throws SQLException {
        String expectedSQL = "DELETE FROM my_test_table WHERE id = ?";

        mockSQLUndoLogAndVerifyExecution(SQLType.INSERT, expectedSQL, mockedPrepStmt -> {
            try {
                verify(mockedPrepStmt).setObject(1, 213, 4);
                verify(mockedPrepStmt).executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Test delete.
     */
    @Test
    public void testDelete() throws SQLException {
        String expectedSQL = "INSERT INTO my_test_table (id, name, since) VALUES (?, ?, ?)";

        mockSQLUndoLogAndVerifyExecution(SQLType.DELETE, expectedSQL, mockedPrepStmt -> {
            try {
                verify(mockedPrepStmt).setObject(1, 213, 4);
                verify(mockedPrepStmt).setObject(2, "SEATA", 12);
                verify(mockedPrepStmt).setObject(3, "2014", 12);
                verify(mockedPrepStmt).executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void mockSQLUndoLogAndVerifyExecution(SQLType sqlType, String expectedSQL,
                                                 Consumer<PreparedStatement> verifier)
            throws SQLException {
        AbstractUndoExecutor executor = UndoExecutorFactory.getUndoExecutor(JdbcConstants.MYSQL, fixture(sqlType));

        Connection mockedConnection = mock(Connection.class);
        PreparedStatement mockedPrepStmt = mock(PreparedStatement.class);

        when(mockedConnection.prepareStatement(expectedSQL)).thenReturn(mockedPrepStmt);

        executor.executeOn(mockedConnection);
        verifier.accept(mockedPrepStmt);
    }

    SQLUndoLog fixture(SQLType sqlType) {
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setTableName("my_test_table");
        sqlUndoLog.setSqlType(sqlType);

        TableRecords beforeImage = new TableRecords(new MockTableMeta("product", "id"));

        Row beforeRow = new Row();

        Field pkField = new Field();
        pkField.setKeyType(KeyType.PrimaryKey);
        pkField.setName("id");
        pkField.setType(Types.INTEGER);
        pkField.setValue(213);
        beforeRow.add(pkField);

        Field name = new Field();
        name.setName("name");
        name.setType(Types.VARCHAR);
        name.setValue("SEATA");
        beforeRow.add(name);

        Field since = new Field();
        since.setName("since");
        since.setType(Types.VARCHAR);
        since.setValue("2014");
        beforeRow.add(since);

        beforeImage.add(beforeRow);

        TableRecords afterImage = new TableRecords(new MockTableMeta("product", "id"));

        Row afterRow = new Row();

        Field pkField1 = new Field();
        pkField1.setKeyType(KeyType.PrimaryKey);
        pkField1.setName("id");
        pkField1.setType(Types.INTEGER);
        pkField1.setValue(213);
        afterRow.add(pkField1);

        Field name1 = new Field();
        name1.setName("name");
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

        return sqlUndoLog;
    }


    /**
     * The type Mock table meta.
     */
    public static class MockTableMeta extends TableMeta {

        private String mockPK;

        /**
         * Instantiates a new Mock table meta.
         *
         * @param tableName the table name
         * @param pkName    the pk name
         */
        public MockTableMeta(String tableName, String pkName) {
            setTableName(tableName);
            this.mockPK = pkName;

        }

        @Override
        public String getTableName() {
            return super.getTableName();
        }

        @Override
        public String getPkName() {
            return mockPK;
        }
    }

}
