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
package org.apache.seata.server.storage.db.store;

import org.apache.seata.common.exception.SeataRuntimeException;
import org.apache.seata.core.store.MappingDO;
import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VGroupMappingDaoUnitTest extends BaseSpringBootTest {
    private DataSource source;
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet result;
    private VGroupMappingDataBaseDAO dao;

    @BeforeEach
    void open() throws Exception {
        source = mock(DataSource.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        result = mock(ResultSet.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        when(statement.executeUpdate()).thenReturn(1);
        dao = new VGroupMappingDataBaseDAO(source);
    }

    @Test
    void insertBindsNamespaceAndClusterAndClosesResources() throws Exception {
        MappingDO mapping = new MappingDO();
        mapping.setVGroup("payments");
        mapping.setNamespace("tenant");
        mapping.setCluster("east");
        assertTrue(dao.insertMappingDO(mapping));
        verify(statement).setString(1, "payments");
        verify(statement).setString(2, "tenant");
        verify(statement).setString(3, "east");
        verify(connection).setAutoCommit(true);
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void queryMapsRowsAndDeletesUseGroupPredicate() throws Exception {
        when(result.next()).thenReturn(true, false);
        when(result.getString("namespace")).thenReturn("tenant");
        when(result.getString("cluster")).thenReturn("east");
        when(result.getString("vGroup")).thenReturn("payments");
        List<MappingDO> mappings = dao.queryMappingDO();
        assertEquals(1, mappings.size());
        assertEquals("payments", mappings.get(0).getVGroup());
        assertEquals("tenant", mappings.get(0).getNamespace());
        assertEquals("east", mappings.get(0).getCluster());
        verify(result).close();
        assertTrue(dao.clearMappingDOByVGroup("payments"));
        assertTrue(dao.deleteMappingDOByVGroup("payments"));
        verify(statement, times(2)).setString(1, "payments");
        when(statement.executeUpdate()).thenReturn(0);
        assertFalse(dao.clearMappingDOByVGroup("missing"));
        assertFalse(dao.deleteMappingDOByVGroup("missing"));
    }

    @Test
    void sqlFailuresPropagateAndStillCloseConnection() throws Exception {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("offline"));
        assertThrows(SeataRuntimeException.class, () -> dao.insertMappingDO(new MappingDO()));
        assertThrows(SeataRuntimeException.class, () -> dao.clearMappingDOByVGroup("g"));
        assertThrows(RuntimeException.class, () -> dao.deleteMappingDOByVGroup("g"));
        assertThrows(SeataRuntimeException.class, dao::queryMappingDO);
        verify(connection, times(4)).close();
    }
}
