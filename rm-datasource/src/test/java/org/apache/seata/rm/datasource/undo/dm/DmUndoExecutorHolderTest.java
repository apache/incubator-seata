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
package org.apache.seata.rm.datasource.undo.dm;

import org.apache.seata.rm.datasource.undo.AbstractUndoExecutor;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * The type DmUndoExecutorHolder test.
 */
public class DmUndoExecutorHolderTest {

    private DmUndoExecutorHolder holder;
    private SQLUndoLog sqlUndoLog;

    @BeforeEach
    public void setup() {
        holder = new DmUndoExecutorHolder();

        sqlUndoLog = new SQLUndoLog();
        TableMeta tableMeta = Mockito.mock(TableMeta.class);
        Mockito.when(tableMeta.getTableName()).thenReturn("test_table");

        sqlUndoLog.setTableMeta(tableMeta);
        sqlUndoLog.setTableName("test_table");
    }

    @Test
    public void testGetInsertExecutor() {
        sqlUndoLog.setSqlType(SQLType.INSERT);

        AbstractUndoExecutor executor = holder.getInsertExecutor(sqlUndoLog);

        Assertions.assertNotNull(executor);
        Assertions.assertTrue(executor instanceof DmUndoInsertExecutor);
        Assertions.assertEquals(sqlUndoLog, executor.getSqlUndoLog());
    }

    @Test
    public void testGetUpdateExecutor() {
        sqlUndoLog.setSqlType(SQLType.UPDATE);

        AbstractUndoExecutor executor = holder.getUpdateExecutor(sqlUndoLog);

        Assertions.assertNotNull(executor);
        Assertions.assertTrue(executor instanceof DmUndoUpdateExecutor);
        Assertions.assertEquals(sqlUndoLog, executor.getSqlUndoLog());
    }

    @Test
    public void testGetDeleteExecutor() {
        sqlUndoLog.setSqlType(SQLType.DELETE);

        AbstractUndoExecutor executor = holder.getDeleteExecutor(sqlUndoLog);

        Assertions.assertNotNull(executor);
        Assertions.assertTrue(executor instanceof DmUndoDeleteExecutor);
        Assertions.assertEquals(sqlUndoLog, executor.getSqlUndoLog());
    }

    @Test
    public void testAllExecutorsWithNullUndoLog() {
        // These methods don't throw NullPointerException, they just pass null to constructors
        AbstractUndoExecutor insertExecutor = holder.getInsertExecutor(null);
        AbstractUndoExecutor updateExecutor = holder.getUpdateExecutor(null);
        AbstractUndoExecutor deleteExecutor = holder.getDeleteExecutor(null);

        Assertions.assertNotNull(insertExecutor);
        Assertions.assertNotNull(updateExecutor);
        Assertions.assertNotNull(deleteExecutor);
        Assertions.assertTrue(insertExecutor instanceof DmUndoInsertExecutor);
        Assertions.assertTrue(updateExecutor instanceof DmUndoUpdateExecutor);
        Assertions.assertTrue(deleteExecutor instanceof DmUndoDeleteExecutor);
    }

    @Test
    public void testExecutorReturnsDifferentInstances() {
        sqlUndoLog.setSqlType(SQLType.INSERT);

        AbstractUndoExecutor executor1 = holder.getInsertExecutor(sqlUndoLog);
        AbstractUndoExecutor executor2 = holder.getInsertExecutor(sqlUndoLog);

        Assertions.assertNotSame(executor1, executor2);
        Assertions.assertTrue(executor1 instanceof DmUndoInsertExecutor);
        Assertions.assertTrue(executor2 instanceof DmUndoInsertExecutor);
    }
}
