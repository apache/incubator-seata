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
import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.rm.DefaultResourceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

public class BusinessActionContextUtilTest {

    @AfterEach
    public void tearDown() {
        BusinessActionContextUtil.clear();
    }

    @Test
    public void testContextHolder() {
        BusinessActionContext context = new BusinessActionContext();

        BusinessActionContextUtil.setContext(context);
        assertSame(context, BusinessActionContextUtil.getContext());

        BusinessActionContextUtil.clear();
        assertNull(BusinessActionContextUtil.getContext());
    }

    @Test
    public void testAddContextReturnsFalseForEmptyOrDelayedContext() {
        assertFalse(BusinessActionContextUtil.addContext(Collections.emptyMap()));
        assertFalse(BusinessActionContextUtil.addContext("key", null));

        BusinessActionContext context = newActionContext();
        context.setDelayReport(true);
        BusinessActionContextUtil.setContext(context);

        assertFalse(BusinessActionContextUtil.addContext("name", "seata"));
        assertTrue(context.getUpdated());
        assertEquals("seata", context.getActionContext().get("name"));
    }

    @Test
    public void testAddContextReportsImmediatelyWhenChanged() throws TransactionException {
        BusinessActionContext context = newActionContext();
        BusinessActionContextUtil.setContext(context);
        DefaultResourceManager resourceManager = mock(DefaultResourceManager.class);

        try (MockedStatic<DefaultResourceManager> mocked = mockStatic(DefaultResourceManager.class)) {
            mocked.when(DefaultResourceManager::get).thenReturn(resourceManager);

            assertTrue(BusinessActionContextUtil.addContext("name", "seata"));
        }

        assertNull(context.getUpdated());
        verify(resourceManager)
                .branchReport(eq(BranchType.TCC), eq("xid"), eq(1L), eq(BranchStatus.Registered), anyString());
    }

    @Test
    public void testReportContextSkipsWhenNotUpdated() throws TransactionException {
        BusinessActionContext context = newActionContext();
        DefaultResourceManager resourceManager = mock(DefaultResourceManager.class);

        try (MockedStatic<DefaultResourceManager> mocked = mockStatic(DefaultResourceManager.class)) {
            mocked.when(DefaultResourceManager::get).thenReturn(resourceManager);

            assertFalse(BusinessActionContextUtil.reportContext(context));
        }
    }

    @Test
    public void testReportContextWrapsTransactionException() throws TransactionException {
        BusinessActionContext context = newActionContext();
        context.setUpdated(true);
        DefaultResourceManager resourceManager = mock(DefaultResourceManager.class);

        try (MockedStatic<DefaultResourceManager> mocked = mockStatic(DefaultResourceManager.class)) {
            mocked.when(DefaultResourceManager::get).thenReturn(resourceManager);
            org.mockito.Mockito.doThrow(new TransactionException("failed"))
                    .when(resourceManager)
                    .branchReport(eq(BranchType.TCC), eq("xid"), eq(1L), eq(BranchStatus.Registered), anyString());

            assertThrows(FrameworkException.class, () -> BusinessActionContextUtil.reportContext(context));
        }
    }

    @Test
    public void testGetBusinessActionContext() {
        String applicationData = "{\"" + Constants.TX_ACTION_CONTEXT + "\":{\"name\":\"seata\"}}";

        BusinessActionContext context =
                BusinessActionContextUtil.getBusinessActionContext("xid", 2L, "prepare", applicationData);

        assertEquals("xid", context.getXid());
        assertEquals(2L, context.getBranchId());
        assertEquals("prepare", context.getActionName());
        assertEquals("seata", context.getActionContext("name"));

        BusinessActionContext emptyContext =
                BusinessActionContextUtil.getBusinessActionContext("xid", 3L, "prepare", "");
        assertTrue(emptyContext.getActionContext().isEmpty());
    }

    private static BusinessActionContext newActionContext() {
        Map<String, Object> actionContext = new HashMap<>();
        BusinessActionContext context = new BusinessActionContext("xid", "1", actionContext);
        context.setBranchType(BranchType.TCC);
        return context;
    }
}
