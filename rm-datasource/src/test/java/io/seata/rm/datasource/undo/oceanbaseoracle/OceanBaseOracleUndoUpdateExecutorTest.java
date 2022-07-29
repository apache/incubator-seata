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
package io.seata.rm.datasource.undo.oceanbaseoracle;

import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.BaseExecutorTest;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.SQLType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test cases for undo-update executor of OceanBaseOracle
 *
 * @author hsien999
 */
public class OceanBaseOracleUndoUpdateExecutorTest extends BaseExecutorTest {
    private static OceanBaseOracleUndoUpdateExecutor EXECUTOR;
    private static final String TABLE_NAME = "TABLE_NAME";
    private static final String ID_NAME = "ID";
    private static final String AGE_NAME = "AGE";

    @BeforeAll
    public static void init() {
        TableMeta tableMeta = mock(TableMeta.class);
        when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Collections.singletonList(ID_NAME));
        when(tableMeta.getTableName()).thenReturn(TABLE_NAME);

        // build before image
        TableRecords beforeImage = new TableRecords();
        beforeImage.setTableName(TABLE_NAME);
        beforeImage.setTableMeta(tableMeta);

        List<Row> beforeRows = new ArrayList<>();
        beforeImage.setRows(beforeRows);

        Row row0 = new Row();
        addField(row0, ID_NAME, 1, "1");
        addField(row0, AGE_NAME, 1, "a");
        beforeImage.add(row0);

        Row row1 = new Row();
        addField(row1, ID_NAME, 1, "2");
        addField(row1, AGE_NAME, 1, "b");
        beforeImage.add(row1);

        // build after image
        TableRecords afterImage = new TableRecords();
        afterImage.setTableName(TABLE_NAME);
        afterImage.setTableMeta(tableMeta);

        List<Row> afterRows = new ArrayList<>();
        afterImage.setRows(afterRows);

        Row row2 = new Row();
        addField(row2, ID_NAME, 1, "1");
        addField(row2, AGE_NAME, 1, "c");
        afterRows.add(row2);

        Row row3 = new Row();
        addField(row3, ID_NAME, 1, "2");
        addField(row3, AGE_NAME, 1, "d");
        afterRows.add(row3);

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.UPDATE);
        sqlUndoLog.setTableMeta(tableMeta);
        sqlUndoLog.setTableName(TABLE_NAME);
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);

        EXECUTOR = new OceanBaseOracleUndoUpdateExecutor(sqlUndoLog);
    }

    @Test
    public void testBuildUndoSQL() {
        String sql = EXECUTOR.buildUndoSQL();
        Assertions.assertNotNull(sql);
        Assertions.assertTrue(sql.contains("UPDATE"));
        Assertions.assertTrue(sql.contains(TABLE_NAME));
        Assertions.assertTrue(sql.contains(ID_NAME));
        Assertions.assertEquals("UPDATE TABLE_NAME SET AGE = ? WHERE ID = ? ", sql.toUpperCase());
    }

    @Test
    public void testGetUndoRows() {
        Assertions.assertEquals(EXECUTOR.getUndoRows(), EXECUTOR.getSqlUndoLog().getBeforeImage());
    }
}
