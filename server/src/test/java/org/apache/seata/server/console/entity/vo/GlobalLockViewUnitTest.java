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
package org.apache.seata.server.console.entity.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.core.constants.ServerTableColumnsName;
import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalLockViewUnitTest {
    @Test
    void sqlConversionSerializesLargeIdsAsStringsAndRetainsTimestamps() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(ServerTableColumnsName.LOCK_TABLE_XID)).thenReturn("host:8091:9007199254740993");
        when(rs.getLong(ServerTableColumnsName.LOCK_TABLE_TRANSACTION_ID)).thenReturn(9007199254740993L);
        when(rs.getLong(ServerTableColumnsName.LOCK_TABLE_BRANCH_ID)).thenReturn(9007199254740995L);
        when(rs.getString(ServerTableColumnsName.LOCK_TABLE_RESOURCE_ID)).thenReturn("db");
        when(rs.getString(ServerTableColumnsName.LOCK_TABLE_TABLE_NAME)).thenReturn("orders");
        when(rs.getString(ServerTableColumnsName.LOCK_TABLE_PK)).thenReturn("7");
        when(rs.getString(ServerTableColumnsName.LOCK_TABLE_ROW_KEY)).thenReturn("db^^^orders^^^7");
        when(rs.getTimestamp(ServerTableColumnsName.LOCK_TABLE_GMT_CREATE)).thenReturn(new Timestamp(1000));
        when(rs.getTimestamp(ServerTableColumnsName.LOCK_TABLE_GMT_MODIFIED)).thenReturn(new Timestamp(2000));
        GlobalLockVO view = GlobalLockVO.convert(rs);
        view.setVgroup("payments");
        JsonNode json = new ObjectMapper().valueToTree(view);
        assertTrue(json.get("transactionId").isTextual());
        assertEquals("9007199254740993", json.get("transactionId").asText());
        assertEquals("9007199254740995", json.get("branchId").asText());
        assertEquals("payments", json.get("vgroup").asText());
        assertEquals("orders", json.get("tableName").asText());
        assertEquals("db", json.get("resourceId").asText());
        assertEquals("7", json.get("pk").asText());
        assertEquals(1000, json.get("gmtCreate").asLong());
        assertEquals(2000, json.get("gmtModified").asLong());
        assertTrue(view.toString().contains("9007199254740993"));
        when(rs.getTimestamp(anyString())).thenReturn(null);
        assertNull(GlobalLockVO.convert(rs).getGmtCreate());
        assertNull(GlobalLockVO.convert(rs).getGmtModified());
    }
}
