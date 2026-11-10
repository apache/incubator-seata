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
package org.apache.seata.rm.datasource.undo.mysql;

import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.UUIDGenerator;
import org.apache.seata.core.constants.ClientTableColumnsName;
import org.apache.seata.core.rpc.processor.Pair;
import org.apache.seata.rm.datasource.undo.UndoLogConstants;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MySQLUndoLogManagerBehaviorTest {
    @Test
    void oversizedLogIsSplitAndMainRecordReferencesAllFragments() throws Exception {
        MySQLUndoLogManager manager = new MySQLUndoLogManager();
        Connection connection = mock(Connection.class);
        PreparedStatement part1 = mock(PreparedStatement.class);
        PreparedStatement part2 = mock(PreparedStatement.class);
        PreparedStatement main = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(part1, part2, main);
        byte[] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        try (MockedStatic<UUIDGenerator> ids = mockStatic(UUIDGenerator.class)) {
            ids.when(UUIDGenerator::generateUUID).thenReturn(101L, 102L);
            manager.insertUndoLogWithNormal("xid", 42, "map=5&serializer=jackson", bytes, connection);
        }
        verify(part1).setLong(1, 101L);
        verify(part2).setLong(1, 102L);
        verify(main).setLong(1, 42L);
        assertPayload(part1, new byte[] {4, 5, 6, 7});
        assertPayload(part2, new byte[] {8, 9});
        assertPayload(main, new byte[] {0, 1, 2, 3});
        verify(part1).setString(3, "branchId=42");
        verify(part2).setString(3, "branchId=42");
        org.mockito.ArgumentCaptor<String> context = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(main).setString(eq(3), context.capture());
        Map<String, String> decoded = CollectionUtils.decodeMap(context.getValue());
        assertEquals(
                "101" + UndoLogConstants.SUB_SPLIT_KEY + "102" + UndoLogConstants.SUB_SPLIT_KEY,
                decoded.get(UndoLogConstants.SUB_ID_KEY));
        assertEquals("jackson", decoded.get(UndoLogConstants.SERIALIZER_KEY));
        for (PreparedStatement statement : Arrays.asList(part1, part2, main)) {
            verify(statement).setString(2, "xid");
            verify(statement).setInt(5, 0);
            verify(statement).executeUpdate();
            verify(statement).close();
        }
    }

    private void assertPayload(PreparedStatement statement, byte[] expected) throws Exception {
        org.mockito.ArgumentCaptor<InputStream> payload = org.mockito.ArgumentCaptor.forClass(InputStream.class);
        verify(statement).setBlob(eq(4), payload.capture());
        byte[] actual = new byte[expected.length];
        assertEquals(expected.length, payload.getValue().read(actual));
        assertArrayEquals(expected, actual);
        assertEquals(-1, payload.getValue().read());
    }

    @Test
    void loadsFragmentsWithBoundIdentifiersAndClosesJdbcResources() throws Exception {
        MySQLUndoLogManager manager = new MySQLUndoLogManager();
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet result = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        when(result.next()).thenReturn(true, true, false);
        byte[] first = {1, 2};
        byte[] second = {3};
        when(result.getBytes(ClientTableColumnsName.UNDO_LOG_ROLLBACK_INFO)).thenReturn(first, second);
        Pair<Integer, List<byte[]>> fragments =
                manager.getSubRollbackInfo(connection, "101" + UndoLogConstants.SUB_SPLIT_KEY + "102", 42L, "xid");
        assertEquals(3, fragments.getFirst());
        assertEquals(Arrays.asList(first, second), fragments.getSecond());
        verify(statement).setLong(1, 101L);
        verify(statement).setLong(2, 102L);
        verify(statement).setString(3, "xid");
        verify(result).close();
        verify(statement).close();
        assertEquals(0, manager.getSubRollbackInfo(connection, " ", 42L, "xid").getFirst());
        SQLException invalid =
                assertThrows(SQLException.class, () -> manager.getSubRollbackInfo(connection, "invalid", 42L, "xid"));
        assertInstanceOf(NumberFormatException.class, invalid.getCause());
    }

    @Test
    void driverFailuresAreConvertedWithoutLosingCause() throws Exception {
        MySQLUndoLogManager manager = new MySQLUndoLogManager();
        Connection connection = mock(Connection.class);
        RuntimeException failure = new IllegalStateException("driver");
        when(connection.prepareStatement(anyString())).thenThrow(failure);
        assertSame(
                failure,
                assertThrows(
                                SQLException.class,
                                () -> manager.insertUndoLogWithNormal("xid", 42, "", new byte[0], connection))
                        .getCause());
        assertSame(
                failure,
                assertThrows(SQLException.class, () -> manager.deleteUndoLogByLogCreated(new Date(), 10, connection))
                        .getCause());
        SQLException sql = new SQLException("SQL");
        doThrow(sql).when(connection).prepareStatement(anyString());
        assertSame(
                sql, assertThrows(SQLException.class, () -> manager.getSubRollbackInfo(connection, "101", 42L, "xid")));
    }
}
