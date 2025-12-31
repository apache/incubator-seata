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
package org.apache.seata.rm.datasource;

import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.sqlparser.SQLType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Savepoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ConnectionContext}.
 */
public class ConnectionContextTest {

    private ConnectionContext connectionContext;

    @BeforeEach
    public void setUp() {
        connectionContext = new ConnectionContext();
    }

    @Test
    public void testInGlobalTransactionReturnsFalseWhenXidIsNull() {
        assertFalse(connectionContext.inGlobalTransaction());
    }

    @Test
    public void testInGlobalTransactionReturnsTrueWhenXidIsSet() {
        connectionContext.setXid("test-xid-123");
        assertTrue(connectionContext.inGlobalTransaction());
    }

    @Test
    public void testIsBranchRegisteredReturnsFalseWhenBranchIdIsNull() {
        assertFalse(connectionContext.isBranchRegistered());
    }

    @Test
    public void testIsBranchRegisteredReturnsTrueWhenBranchIdIsSet() {
        connectionContext.setBranchId(12345L);
        assertTrue(connectionContext.isBranchRegistered());
    }

    @Test
    public void testBindThrowsExceptionWhenXidIsNull() {
        assertThrows(IllegalArgumentException.class, () -> connectionContext.bind(null));
    }

    @Test
    public void testBindSetsXidWhenNotInGlobalTransaction() {
        connectionContext.bind("test-xid-456");
        assertEquals("test-xid-456", connectionContext.getXid());
    }

    @Test
    public void testBindThrowsExceptionWhenBindingDifferentXid() {
        connectionContext.bind("xid-1");
        assertThrows(ShouldNeverHappenException.class, () -> connectionContext.bind("xid-2"));
    }

    @Test
    public void testBindSucceedsWhenBindingSameXid() {
        connectionContext.bind("same-xid");
        connectionContext.bind("same-xid");
        assertEquals("same-xid", connectionContext.getXid());
    }

    @Test
    public void testHasUndoLogReturnsFalseWhenEmpty() {
        assertFalse(connectionContext.hasUndoLog());
    }

    @Test
    public void testHasUndoLogReturnsTrueWhenUndoLogAppended() {
        SQLUndoLog undoLog = new SQLUndoLog();
        undoLog.setSqlType(SQLType.INSERT);
        undoLog.setTableName("test_table");
        connectionContext.appendUndoItem(undoLog);
        assertTrue(connectionContext.hasUndoLog());
    }

    @Test
    public void testHasLockKeyReturnsFalseWhenEmpty() {
        assertFalse(connectionContext.hasLockKey());
    }

    @Test
    public void testHasLockKeyReturnsTrueWhenLockKeyAppended() {
        connectionContext.appendLockKey("test_table:1");
        assertTrue(connectionContext.hasLockKey());
    }

    @Test
    public void testBuildLockKeysReturnsNullWhenEmpty() {
        assertNull(connectionContext.buildLockKeys());
    }

    @Test
    public void testBuildLockKeysReturnsConcatenatedKeys() {
        connectionContext.appendLockKey("table1:1");
        connectionContext.appendLockKey("table1:2");
        String lockKeys = connectionContext.buildLockKeys();
        assertNotNull(lockKeys);
        assertTrue(lockKeys.contains("table1:1"));
        assertTrue(lockKeys.contains("table1:2"));
    }

    @Test
    public void testGetUndoItemsReturnsEmptyListWhenNoItems() {
        assertTrue(connectionContext.getUndoItems().isEmpty());
    }

    @Test
    public void testGetUndoItemsReturnsAppendedItems() {
        SQLUndoLog undoLog1 = new SQLUndoLog();
        undoLog1.setSqlType(SQLType.INSERT);
        undoLog1.setTableName("table1");

        SQLUndoLog undoLog2 = new SQLUndoLog();
        undoLog2.setSqlType(SQLType.UPDATE);
        undoLog2.setTableName("table2");

        connectionContext.appendUndoItem(undoLog1);
        connectionContext.appendUndoItem(undoLog2);

        assertEquals(2, connectionContext.getUndoItems().size());
    }

    @Test
    public void testResetClearsAllState() {
        connectionContext.setXid("test-xid");
        connectionContext.setBranchId(123L);
        connectionContext.setGlobalLockRequire(true);
        connectionContext.appendLockKey("table:1");

        SQLUndoLog undoLog = new SQLUndoLog();
        undoLog.setSqlType(SQLType.INSERT);
        connectionContext.appendUndoItem(undoLog);

        connectionContext.reset();

        assertNull(connectionContext.getXid());
        assertNull(connectionContext.getBranchId());
        assertFalse(connectionContext.isGlobalLockRequire());
        assertFalse(connectionContext.hasLockKey());
        assertFalse(connectionContext.hasUndoLog());
    }

    @Test
    public void testResetWithXidSetsNewXid() {
        connectionContext.setXid("old-xid");
        connectionContext.reset("new-xid");
        assertEquals("new-xid", connectionContext.getXid());
    }

    @Test
    public void testAutoCommitChangedDefaultIsFalse() {
        assertFalse(connectionContext.isAutoCommitChanged());
    }

    @Test
    public void testSetAutoCommitChanged() {
        connectionContext.setAutoCommitChanged(true);
        assertTrue(connectionContext.isAutoCommitChanged());
    }

    @Test
    public void testGlobalLockRequireDefaultIsFalse() {
        assertFalse(connectionContext.isGlobalLockRequire());
    }

    @Test
    public void testSetGlobalLockRequire() {
        connectionContext.setGlobalLockRequire(true);
        assertTrue(connectionContext.isGlobalLockRequire());
    }

    @Test
    public void testAppendSavepointAndRemove() throws SQLException {
        Savepoint savepoint = createSavepoint("sp1");
        connectionContext.appendSavepoint(savepoint);
        connectionContext.appendLockKey("table:1");

        assertTrue(connectionContext.hasLockKey());

        connectionContext.removeSavepoint(savepoint);
        assertFalse(connectionContext.hasLockKey());
    }

    @Test
    public void testReleaseSavepointMovesDataToCurrentSavepoint() throws SQLException {
        Savepoint sp1 = createSavepoint("sp1");
        connectionContext.appendSavepoint(sp1);
        connectionContext.appendLockKey("table:1");

        Savepoint sp2 = createSavepoint("sp2");
        connectionContext.appendSavepoint(sp2);
        connectionContext.appendLockKey("table:2");

        connectionContext.releaseSavepoint(sp1);

        assertTrue(connectionContext.hasLockKey());
        String lockKeys = connectionContext.buildLockKeys();
        assertTrue(lockKeys.contains("table:1"));
        assertTrue(lockKeys.contains("table:2"));
    }

    @Test
    public void testRemoveSavepointWithNullClearsAll() {
        connectionContext.appendLockKey("table:1");

        SQLUndoLog undoLog = new SQLUndoLog();
        undoLog.setSqlType(SQLType.INSERT);
        connectionContext.appendUndoItem(undoLog);

        connectionContext.removeSavepoint(null);

        assertFalse(connectionContext.hasLockKey());
        assertFalse(connectionContext.hasUndoLog());
    }

    @Test
    public void testToStringReturnsNonNull() {
        assertNotNull(connectionContext.toString());
    }

    private Savepoint createSavepoint(String name) {
        return new Savepoint() {
            @Override
            public int getSavepointId() throws SQLException {
                return name.hashCode();
            }

            @Override
            public String getSavepointName() throws SQLException {
                return name;
            }
        };
    }
}
