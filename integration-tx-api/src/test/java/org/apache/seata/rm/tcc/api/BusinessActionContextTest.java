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
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for BusinessActionContext action status methods.
 */
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
}
