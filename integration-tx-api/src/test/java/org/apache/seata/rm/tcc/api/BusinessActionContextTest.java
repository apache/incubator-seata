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
package org.apache.seata.rm.tcc.api;

import org.apache.seata.common.Constants;
import org.apache.seata.core.model.BranchType;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BusinessActionContextTest {

    @Test
    public void testGetActionStatusWhenContextIsNull() {
        BusinessActionContext context = new BusinessActionContext();
        assertNull(context.getActionStatus(), "Action status should be null when actionContext is null");
    }

    @Test
    public void testGetActionStatusWhenNotSet() {
        BusinessActionContext context = new BusinessActionContext();
        Map<String, Object> actionContext = new HashMap<>();
        context.setActionContext(actionContext);

        assertNull(context.getActionStatus(), "Action status should be null when not set");
    }

    @Test
    public void testSetAndGetActionStatusSuccess() {
        BusinessActionContext context = new BusinessActionContext();
        Map<String, Object> actionContext = new HashMap<>();
        context.setActionContext(actionContext);

        context.setActionStatus(Constants.ACTION_STATUS_SUCCESS);

        assertEquals(Constants.ACTION_STATUS_SUCCESS, context.getActionStatus(), "Action status should be 'success'");
    }

    @Test
    public void testSetAndGetActionStatusFailed() {
        BusinessActionContext context = new BusinessActionContext();
        Map<String, Object> actionContext = new HashMap<>();
        context.setActionContext(actionContext);

        context.setActionStatus(Constants.ACTION_STATUS_FAILED);

        assertEquals(Constants.ACTION_STATUS_FAILED, context.getActionStatus(), "Action status should be 'failed'");
    }

    @Test
    public void testSetActionStatusWithNullStatus() {
        BusinessActionContext context = new BusinessActionContext();
        Map<String, Object> actionContext = new HashMap<>();
        context.setActionContext(actionContext);

        context.setActionStatus(Constants.ACTION_STATUS_SUCCESS);
        context.setActionStatus(null);

        assertEquals(
                Constants.ACTION_STATUS_SUCCESS,
                context.getActionStatus(),
                "Action status should not change when setting null");
    }

    @Test
    public void testSetActionStatusWithNullActionContext() {
        BusinessActionContext context = new BusinessActionContext();

        // Should not throw exception
        assertDoesNotThrow(
                () -> context.setActionStatus(Constants.ACTION_STATUS_SUCCESS),
                "Setting action status with null actionContext should not throw exception");

        assertNull(context.getActionStatus(), "Action status should still be null");
    }

    @Test
    public void testActionStatusOverwrite() {
        BusinessActionContext context = new BusinessActionContext();
        Map<String, Object> actionContext = new HashMap<>();
        context.setActionContext(actionContext);

        context.setActionStatus(Constants.ACTION_STATUS_SUCCESS);
        assertEquals(Constants.ACTION_STATUS_SUCCESS, context.getActionStatus());

        context.setActionStatus(Constants.ACTION_STATUS_FAILED);
        assertEquals(
                Constants.ACTION_STATUS_FAILED,
                context.getActionStatus(),
                "Action status should be overwritten to 'failed'");
    }

    @Test
    public void testBranchIdAccessors() {
        BusinessActionContext context = new BusinessActionContext();

        assertEquals(-1, context.getBranchId());

        context.setBranchId(10L);
        assertEquals(10L, context.getBranchId());

        context.setBranchId("11");
        assertEquals(11L, context.getBranchId());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testAddActionContextUpdatesFlagOnlyWhenChanged() {
        Map<String, Object> actionContext = new HashMap<>();
        BusinessActionContext context = new BusinessActionContext("xid", "1", actionContext);

        assertFalse(context.addActionContext("name", null));
        assertNull(context.getUpdated());

        assertTrue(context.addActionContext("name", "seata"));
        assertTrue(context.getUpdated());
        context.setUpdated(null);

        assertFalse(context.addActionContext("name", "seata"));
        assertNull(context.getUpdated());
    }

    @Test
    public void testGetActionContextWithTargetClass() {
        Map<String, Object> actionContext = new HashMap<>();
        actionContext.put("count", 3);
        actionContext.put("payload", "{\"name\":\"payload-name\"}");
        BusinessActionContext context = new BusinessActionContext("xid", "2", actionContext);

        assertEquals(3, context.getActionContext("count", Integer.class));
        assertEquals(
                "payload-name",
                context.getActionContext("payload", Payload.class).getName());
    }

    @Test
    public void testTrackedActionContextMarksUpdatedOnMutationOperations() {
        BusinessActionContext context = new BusinessActionContext("xid", "4", new HashMap<>());
        context.enableActionContextTracking();
        context.setActionContext(new HashMap<>());

        context.getActionContext().put("name", "seata");
        assertTrue(context.getUpdated());

        context.setUpdated(null);
        context.getActionContext().putAll(Collections.singletonMap("status", "prepared"));
        assertTrue(context.getUpdated());

        context.setUpdated(null);
        context.getActionContext().remove("name");
        assertTrue(context.getUpdated());

        context.setUpdated(null);
        context.getActionContext().put("name", "seata");
        context.getActionContext().replace("name", "seata-updated");
        assertTrue(context.getUpdated());

        context.setUpdated(null);
        context.getActionContext().computeIfAbsent("branch", key -> "branch-1");
        assertTrue(context.getUpdated());

        context.setUpdated(null);
        context.getActionContext().merge("branch", "branch-2", (oldValue, newValue) -> newValue);
        assertTrue(context.getUpdated());

        context.setUpdated(null);
        context.getActionContext().replaceAll((key, value) -> value);
        assertNull(context.getUpdated());

        context.getActionContext().clear();
        assertTrue(context.getUpdated());
    }

    @Test
    public void testTrackedActionContextMapViewOperations() {
        BusinessActionContext context = new BusinessActionContext();
        context.enableActionContextTracking();
        context.enableActionContextTracking();

        assertEquals(0, context.getActionContext().size());
        assertFalse(context.getActionContext().containsKey("missing"));
        assertFalse(context.getActionContext().containsValue("missing"));

        context.getActionContext().putAll(Collections.emptyMap());
        assertNull(context.getUpdated());

        context.getActionContext().put("name", "seata");
        context.setUpdated(null);
        assertEquals("seata", context.getActionContext().put("name", "seata"));
        assertNull(context.getUpdated());

        assertEquals("seata", context.getActionContext().get("name"));
        context.setUpdated(null);
        assertNull(context.getActionContext().remove("missing"));
        assertNull(context.getUpdated());

        context.setActionContext(Collections.singletonMap("entry", "old"));
        Map.Entry<String, Object> entry =
                context.getActionContext().entrySet().iterator().next();
        assertEquals("entry", entry.getKey());
        assertEquals("old", entry.getValue());

        assertEquals("old", entry.setValue("new"));
        assertTrue(context.getUpdated());

        context.setUpdated(null);
        assertEquals("new", entry.setValue("new"));
        assertNull(context.getUpdated());
        assertEquals(1, context.getActionContext().entrySet().size());
        assertEquals(entry, context.getActionContext().entrySet().iterator().next());
        assertEquals(
                entry.hashCode(),
                context.getActionContext().entrySet().iterator().next().hashCode());

        context.setUpdated(null);
        assertFalse(context.getActionContext().entrySet().remove("not-entry"));
        assertNull(context.getUpdated());

        assertFalse(context.getActionContext().entrySet().remove(new AbstractMap.SimpleEntry<>("missing", "new")));
        assertNull(context.getUpdated());

        assertFalse(context.getActionContext().entrySet().remove(new AbstractMap.SimpleEntry<>("entry", "wrong")));
        assertNull(context.getUpdated());

        assertTrue(context.getActionContext().entrySet().remove(new AbstractMap.SimpleEntry<>("entry", "new")));
        assertTrue(context.getUpdated());

        context.setActionContext(Collections.singletonMap("remove", "value"));
        context.setUpdated(null);
        Iterator<Map.Entry<String, Object>> iterator =
                context.getActionContext().entrySet().iterator();
        assertTrue(iterator.hasNext());
        iterator.next();
        iterator.remove();
        assertTrue(context.getUpdated());
        assertEquals(0, context.getActionContext().size());
        context.setUpdated(null);
        context.getActionContext().clear();
        assertNull(context.getUpdated());

        context.setActionContext(null);
        assertEquals(0, context.getActionContext().size());
        context.getActionContext().put("created", "value");
        assertTrue(context.getUpdated());
    }

    @Test
    public void testToStringIncludesCoreFields() {
        BusinessActionContext context = new BusinessActionContext("xid", "3", new HashMap<>());
        context.setActionName("prepare");
        context.setDelayReport(true);
        context.setUpdated(false);
        context.setBranchType(BranchType.TCC);

        String value = context.toString();

        assertTrue(value.contains("xid:xid"));
        assertTrue(value.contains("branch_Id:3"));
        assertTrue(value.contains("action_name:prepare"));
        assertTrue(value.contains("branch_type:TCC"));
    }

    public static class Payload {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
