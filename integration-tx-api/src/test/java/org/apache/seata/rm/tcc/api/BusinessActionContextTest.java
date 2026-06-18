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

import org.apache.seata.core.model.BranchType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BusinessActionContextTest {

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
