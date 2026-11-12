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
package org.apache.seata.mcp.entity;

import org.apache.seata.mcp.core.utils.DateUtils;
import org.apache.seata.mcp.entity.vo.McpBranchSessionVO;
import org.apache.seata.mcp.entity.vo.McpGlobalLockVO;
import org.apache.seata.mcp.entity.vo.McpGlobalSessionVO;
import org.apache.seata.mcp.exception.ServiceCallException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class McpResponseBehaviorTest {
    @Test
    void lockResponseConvertsTimestampsAndPreservesIdentifiers() {
        ObjectMapper mapper = new ObjectMapper();
        McpGlobalLockVO lock = mapper.readValue(
                "{\"xid\":\"x\",\"transactionId\":\"t\",\"branchId\":\"b\",\"resourceId\":\"r\",\"tableName\":\"orders\",\"pk\":\"42\",\"rowKey\":\"row\",\"vgroup\":\"group\",\"gmtCreate\":0,\"gmtModified\":1000}",
                McpGlobalLockVO.class);
        assertEquals("x", lock.getXid());
        assertEquals("t", lock.getTransactionId());
        assertEquals("b", lock.getBranchId());
        assertEquals("r", lock.getResourceId());
        assertEquals("orders", lock.getTableName());
        assertEquals("42", lock.getPk());
        assertEquals("row", lock.getRowKey());
        assertEquals("group", lock.getVgroup());
        assertEquals(DateUtils.convertToDateTimeFromTimestamp(0L), lock.getGmtCreate());
        assertEquals(DateUtils.convertToDateTimeFromTimestamp(1000L), lock.getGmtModified());
        lock.setGmtCreate("created");
        lock.setGmtModified("modified");
        assertEquals("created", lock.getGmtCreate());
        assertEquals("modified", lock.getGmtModified());
        lock.setXid("updated");
        lock.setTransactionId("updated");
        lock.setBranchId("updated");
        lock.setResourceId("updated");
        lock.setTableName("updated");
        lock.setPk("updated");
        lock.setRowKey("updated");
        lock.setVgroup("updated");
        assertEquals(
                "updated",
                mapper.readTree(mapper.writeValueAsString(lock)).get("xid").asText());
    }

    @Test
    void sessionResponseUsesPublicJsonPropertyNames() {
        McpBranchSessionVO branch = new McpBranchSessionVO();
        branch.setCreateTime("created");
        branch.setModifiedTime("modified");
        assertEquals("created", branch.getCreateTime());
        assertEquals("modified", branch.getModifiedTime());
        McpGlobalSessionVO session = new McpGlobalSessionVO();
        session.setBeginTime("begin");
        session.setCreateTime("created");
        session.setModifiedTime("modified");
        session.setMcpBranchSessionVOS(Set.of(branch));
        assertEquals("begin", session.getBegin());
        assertEquals("created", session.getCreateTime());
        assertEquals("modified", session.getModifiedTime());
        assertEquals(Set.of(branch), session.getMcpBranchSessionVOS());
        tools.jackson.databind.JsonNode json = new ObjectMapper().valueToTree(session);
        assertEquals("created", json.get("gmtCreate").asText());
        assertEquals(
                "modified",
                json.get("branchSessionVOs").get(0).get("gmtModified").asText());
    }

    @Test
    void serviceErrorsRetainStatusCauseAndTimestamp() {
        IllegalStateException cause = new IllegalStateException("cause");
        ServiceCallException wrapped = new ServiceCallException("failed", cause);
        assertSame(cause, wrapped.getCause());
        assertNull(wrapped.toErrorResponse().get("httpStatus"));
        Map<String, Object> response =
                new ServiceCallException("unavailable", HttpStatus.SERVICE_UNAVAILABLE).toErrorResponse();
        assertEquals(503, response.get("httpStatus"));
        assertEquals("unavailable", response.get("message"));
        assertNotNull(java.time.Instant.parse((String) response.get("timestamp")));
        assertNull(new ServiceCallException("failed").toErrorResponse().get("httpStatus"));
    }
}
