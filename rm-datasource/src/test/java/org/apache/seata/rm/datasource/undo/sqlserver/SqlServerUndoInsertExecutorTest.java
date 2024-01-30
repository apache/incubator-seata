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
package org.apache.seata.rm.datasource.undo.sqlserver;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.alibaba.druid.mock.MockPreparedStatement;
import org.apache.seata.rm.datasource.mock.MockConnection;
import org.apache.seata.rm.datasource.mock.MockDriver;
import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.BaseExecutorTest;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.sqlparser.SQLType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


public class SqlServerUndoInsertExecutorTest extends BaseExecutorTest {
    private static SqlServerUndoInsertExecutor executor;

    @BeforeAll
    public static void init() {
        TableMeta tableMeta = Mockito.mock(TableMeta.class);
        Mockito.when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Collections.singletonList("id"));
        Mockito.when(tableMeta.getTableName()).thenReturn("table_name");

        TableRecords beforeImage = new TableRecords();
        beforeImage.setTableName("table_name");
        beforeImage.setTableMeta(tableMeta);
        List<Row> beforeRows = new ArrayList<>();
        Row row0 = new Row();
        addField(row0, "id", 1, "12345");
        addField(row0, "age", 1, "1");
        beforeRows.add(row0);
        Row row1 = new Row();
        addField(row1, "id", 1, "12346");
        addField(row1, "age", 1, "1");
        beforeRows.add(row1);
        beforeImage.setRows(beforeRows);

        TableRecords afterImage = new TableRecords();
        afterImage.setTableName("table_name");
        afterImage.setTableMeta(tableMeta);
        List<Row> afterRows = new ArrayList<>();
        Row row2 = new Row();
        addField(row2, "id", 1, "12345");
        addField(row2, "age", 1, "2");
        afterRows.add(row2);
        Row row3 = new Row();
        addField(row3, "id", 1, "12346");
        addField(row3, "age", 1, "2");
        afterRows.add(row3);
        afterImage.setRows(afterRows);

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.INSERT);
        sqlUndoLog.setTableMeta(tableMeta);
        sqlUndoLog.setTableName("table_name");
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);

        executor = new SqlServerUndoInsertExecutor(sqlUndoLog);
    }

    @Test
    public void buildUndoSQL() {
        String sql = executor.buildUndoSQL().toUpperCase();
        Assertions.assertNotNull(sql);
        Assertions.assertTrue(sql.contains("DELETE"));
        Assertions.assertTrue(sql.contains("TABLE_NAME"));
        Assertions.assertTrue(sql.contains("ID"));
    }

    @Test
    public void getUndoRows() {
        Assertions.assertEquals(executor.getUndoRows(), executor.getSqlUndoLog().getAfterImage());
    }

    @Test
    public void undoPrepareTest() throws SQLException {
        String sql = executor.buildUndoSQL().toUpperCase();
        MockConnection connection = new MockConnection(new MockDriver(), "", null);
        MockPreparedStatement undoPST = (MockPreparedStatement) connection.prepareStatement(sql);

        List<Field> fieldList = new ArrayList<>();
        fieldList.add(new Field("id", 1, "12345"));
        executor.undoPrepare(undoPST, new ArrayList<>(), fieldList);
        Assertions.assertEquals(1, undoPST.getParameters().size());
    }
}
