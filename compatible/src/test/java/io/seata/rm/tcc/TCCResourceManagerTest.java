/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 *
 *
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
package io.seata.rm.tcc;

import io.seata.core.model.BranchType;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for TCCResourceManager.
 */
public class TCCResourceManagerTest {

    private TCCResourceManager tccResourceManager;

    @BeforeEach
    public void setUp() {
        tccResourceManager = new TCCResourceManager();
    }

    @Test
    public void testExtendsApacheTCCResourceManager() {
        assertTrue(
                org.apache.seata.rm.tcc.TCCResourceManager.class.isAssignableFrom(TCCResourceManager.class),
                "TCCResourceManager should extend org.apache.seata.rm.tcc.TCCResourceManager");
    }

    @Test
    public void testGetBranchType() {
        // The getBranchType() returns org.apache.seata.core.model.BranchType,
        // but io.seata.core.model.BranchType should be compatible
        org.apache.seata.core.model.BranchType actualType = tccResourceManager.getBranchType();
        assertEquals(org.apache.seata.core.model.BranchType.TCC, actualType);
        // Also verify the compatible type works
        assertEquals(BranchType.TCC.name(), actualType.name());
    }

    @Test
    public void testGetTwoPhaseMethodParamsWithApacheBusinessActionContext() {
        String[] keys = {"param1", "param2"};
        Class<?>[] argsClasses = {io.seata.rm.tcc.api.BusinessActionContext.class, String.class};

        BusinessActionContext businessActionContext = new BusinessActionContext();
        businessActionContext.setXid("test-xid");
        businessActionContext.setActionName("testAction");
        businessActionContext.setBranchId(12345L);
        businessActionContext.setDelayReport(false);

        // Initialize actionContext map properly
        java.util.Map<String, Object> actionContextMap = new java.util.HashMap<>();
        actionContextMap.put("param1", "value1");
        actionContextMap.put("param2", "value2");
        businessActionContext.setActionContext(actionContextMap);

        Object[] params = tccResourceManager.getTwoPhaseMethodParams(keys, argsClasses, businessActionContext);

        assertNotNull(params);
        assertEquals(2, params.length);
        assertTrue(params[0] instanceof io.seata.rm.tcc.api.BusinessActionContext);

        io.seata.rm.tcc.api.BusinessActionContext oldContext = (io.seata.rm.tcc.api.BusinessActionContext) params[0];
        assertEquals("test-xid", oldContext.getXid());
        assertEquals("testAction", oldContext.getActionName());
        assertEquals(12345L, oldContext.getBranchId());
    }

    @Test
    public void testGetTwoPhaseMethodParamsWithApacheSeataBusinessActionContext() {
        String[] keys = {"userId", "orderId"};
        Class<?>[] argsClasses = {BusinessActionContext.class, String.class};

        BusinessActionContext businessActionContext = new BusinessActionContext();
        businessActionContext.setXid("test-xid-2");
        businessActionContext.setActionName("orderAction");
        businessActionContext.setBranchId(67890L);

        // Initialize actionContext map properly
        java.util.Map<String, Object> actionContextMap = new java.util.HashMap<>();
        actionContextMap.put("userId", "user123");
        actionContextMap.put("orderId", "order456");
        businessActionContext.setActionContext(actionContextMap);

        Object[] params = tccResourceManager.getTwoPhaseMethodParams(keys, argsClasses, businessActionContext);

        assertNotNull(params);
        assertEquals(2, params.length);
        assertTrue(params[0] instanceof BusinessActionContext);

        BusinessActionContext context = (BusinessActionContext) params[0];
        assertEquals("test-xid-2", context.getXid());
        assertEquals("orderAction", context.getActionName());
        assertEquals(67890L, context.getBranchId());

        // params[1] should be the value of the second key "orderId", not "userId"
        assertEquals("order456", params[1]);
    }

    @Test
    public void testGetTwoPhaseMethodParamsWithMixedTypes() {
        String[] keys = {"amount", "description"};
        Class<?>[] argsClasses = {Integer.class, String.class};

        BusinessActionContext businessActionContext = new BusinessActionContext();

        // Initialize actionContext map properly
        java.util.Map<String, Object> actionContextMap = new java.util.HashMap<>();
        actionContextMap.put("amount", 100);
        actionContextMap.put("description", "test payment");
        businessActionContext.setActionContext(actionContextMap);

        Object[] params = tccResourceManager.getTwoPhaseMethodParams(keys, argsClasses, businessActionContext);

        assertNotNull(params);
        assertEquals(2, params.length);
        assertEquals(100, params[0]);
        assertEquals("test payment", params[1]);
    }

    @Test
    public void testGetTwoPhaseMethodParamsConversion() {
        String[] keys = {};
        Class<?>[] argsClasses = {io.seata.rm.tcc.api.BusinessActionContext.class};

        BusinessActionContext businessActionContext = new BusinessActionContext();
        businessActionContext.setXid("conversion-xid");
        businessActionContext.setActionName("conversionAction");
        businessActionContext.setBranchId(111L);
        businessActionContext.setBranchType(org.apache.seata.core.model.BranchType.TCC);
        businessActionContext.setDelayReport(true);

        java.util.Map<String, Object> actionContext = new java.util.HashMap<>();
        actionContext.put("key1", "value1");
        businessActionContext.setActionContext(actionContext);

        businessActionContext.setUpdated(false);

        Object[] params = tccResourceManager.getTwoPhaseMethodParams(keys, argsClasses, businessActionContext);

        assertNotNull(params);
        assertEquals(1, params.length);
        assertTrue(params[0] instanceof io.seata.rm.tcc.api.BusinessActionContext);

        io.seata.rm.tcc.api.BusinessActionContext converted = (io.seata.rm.tcc.api.BusinessActionContext) params[0];
        assertEquals("conversion-xid", converted.getXid());
        assertEquals("conversionAction", converted.getActionName());
        assertEquals(111L, converted.getBranchId());
        // getBranchType() returns an object (could be String or BranchType), compare by toString()
        Object branchType = converted.getBranchType();
        assertNotNull(branchType);
        assertEquals("TCC", branchType.toString());
        assertTrue(converted.getDelayReport());
        assertNotNull(converted.getActionContext());
        assertEquals("value1", converted.getActionContext("key1"));
    }

    @Test
    public void testGetTwoPhaseMethodParamsEmptyArgs() {
        String[] keys = {};
        Class<?>[] argsClasses = {};

        BusinessActionContext businessActionContext = new BusinessActionContext();

        Object[] params = tccResourceManager.getTwoPhaseMethodParams(keys, argsClasses, businessActionContext);

        assertNotNull(params);
        assertEquals(0, params.length);
    }
}
