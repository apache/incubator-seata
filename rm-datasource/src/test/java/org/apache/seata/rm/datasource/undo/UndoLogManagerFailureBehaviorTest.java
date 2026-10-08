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
package org.apache.seata.rm.datasource.undo;

import org.apache.seata.rm.datasource.undo.dm.DmUndoLogManager;
import org.apache.seata.rm.datasource.undo.kingbase.KingbaseUndoLogManager;
import org.apache.seata.rm.datasource.undo.oracle.OracleUndoLogManager;
import org.apache.seata.rm.datasource.undo.oscar.OscarUndoLogManager;
import org.apache.seata.rm.datasource.undo.postgresql.PostgresqlUndoLogManager;
import org.apache.seata.rm.datasource.undo.sqlserver.SqlServerUndoLogManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UndoLogManagerFailureBehaviorTest {
    static Stream<AbstractUndoLogManager> dialects() {
        return Stream.of(
                new DmUndoLogManager(),
                new KingbaseUndoLogManager(),
                new OracleUndoLogManager(),
                new OscarUndoLogManager(),
                new PostgresqlUndoLogManager(),
                new SqlServerUndoLogManager());
    }

    @ParameterizedTest
    @MethodSource("dialects")
    void insertAndCleanupPreserveSqlErrorsAndWrapUnexpectedDriverErrors(AbstractUndoLogManager manager)
            throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        RuntimeException unexpected = new IllegalStateException("driver failure");
        doThrow(unexpected).when(statement).executeUpdate();
        assertSame(
                unexpected,
                assertThrows(
                                SQLException.class,
                                () -> manager.insertUndoLogWithNormal("xid", 42, "", new byte[0], connection))
                        .getCause());
        assertSame(
                unexpected,
                assertThrows(SQLException.class, () -> manager.deleteUndoLogByLogCreated(new Date(), 10, connection))
                        .getCause());
        verify(statement, times(2)).close();
        SQLException sql = new SQLException("SQL failure", "42000", 100);
        doThrow(sql).when(statement).executeUpdate();
        assertSame(
                sql,
                assertThrows(
                        SQLException.class,
                        () -> manager.insertUndoLogWithNormal("xid", 42, "", new byte[0], connection)));
        assertSame(
                sql,
                assertThrows(SQLException.class, () -> manager.deleteUndoLogByLogCreated(new Date(), 10, connection)));
        verify(statement, times(4)).close();
        doThrow(unexpected).when(statement).executeUpdate();
        assertSame(
                unexpected,
                assertThrows(SQLException.class, () -> manager.deleteUndoLog("xid", 42, connection))
                        .getCause());
        assertSame(
                unexpected,
                assertThrows(
                                SQLException.class,
                                () -> manager.batchDeleteUndoLog(
                                        Collections.singleton("xid"), Collections.singleton(42L), connection))
                        .getCause());
        manager.batchDeleteUndoLog(Collections.emptySet(), Collections.singleton(42L), connection);
    }
}
