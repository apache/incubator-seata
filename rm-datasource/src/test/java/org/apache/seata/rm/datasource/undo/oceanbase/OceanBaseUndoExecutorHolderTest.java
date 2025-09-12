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
package org.apache.seata.rm.datasource.undo.oceanbase;

import org.apache.seata.rm.datasource.undo.AbstractUndoExecutor;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OceanBaseUndoExecutorHolderTest {

    private OceanBaseUndoExecutorHolder executorHolder;

    @Test
    void testGetInsertExecutor() {
        // Arrange
        SQLUndoLog mockUndoLog = mock(SQLUndoLog.class);
        executorHolder = new OceanBaseUndoExecutorHolder();

        // Act
        AbstractUndoExecutor insertExecutor = executorHolder.getInsertExecutor(mockUndoLog);

        // Assert
        assertNotNull(insertExecutor);
        assertTrue(insertExecutor instanceof OceanBaseUndoInsertExecutor);
    }

    @Test
    void testGetUpdateExecutor() {
        // Arrange
        SQLUndoLog mockUndoLog = mock(SQLUndoLog.class);
        executorHolder = new OceanBaseUndoExecutorHolder();

        // Act
        AbstractUndoExecutor updateExecutor = executorHolder.getUpdateExecutor(mockUndoLog);

        // Assert
        assertNotNull(updateExecutor);
        assertTrue(updateExecutor instanceof OceanBaseUndoUpdateExecutor);
    }

    @Test
    void testGetDeleteExecutor() {
        // Arrange
        SQLUndoLog mockUndoLog = mock(SQLUndoLog.class);
        executorHolder = new OceanBaseUndoExecutorHolder();

        // Act
        AbstractUndoExecutor deleteExecutor = executorHolder.getDeleteExecutor(mockUndoLog);

        // Assert
        assertNotNull(deleteExecutor);
        assertTrue(deleteExecutor instanceof OceanBaseUndoDeleteExecutor);
    }

    @Test
    void testGetInsertExecutor_WithNullUndoLog() {
        // Arrange
        executorHolder = new OceanBaseUndoExecutorHolder();

        // Act
        AbstractUndoExecutor insertExecutor = executorHolder.getInsertExecutor(null);

        // Assert
        assertNotNull(insertExecutor);
        assertTrue(insertExecutor instanceof OceanBaseUndoInsertExecutor);
    }

    @Test
    void testGetUpdateExecutor_WithNullUndoLog() {
        // Arrange
        executorHolder = new OceanBaseUndoExecutorHolder();

        // Act
        AbstractUndoExecutor updateExecutor = executorHolder.getUpdateExecutor(null);

        // Assert
        assertNotNull(updateExecutor);
        assertTrue(updateExecutor instanceof OceanBaseUndoUpdateExecutor);
    }

    @Test
    void testGetDeleteExecutor_WithNullUndoLog() {
        // Arrange
        executorHolder = new OceanBaseUndoExecutorHolder();

        // Act
        AbstractUndoExecutor deleteExecutor = executorHolder.getDeleteExecutor(null);

        // Assert
        assertNotNull(deleteExecutor);
        assertTrue(deleteExecutor instanceof OceanBaseUndoDeleteExecutor);
    }
}
