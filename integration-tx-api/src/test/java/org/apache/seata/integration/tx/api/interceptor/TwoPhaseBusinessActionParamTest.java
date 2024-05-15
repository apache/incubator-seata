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
package org.apache.seata.integration.tx.api.interceptor;

import java.util.HashMap;
import java.util.Map;

import org.apache.seata.common.Constants;
import org.apache.seata.core.model.BranchType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TwoPhaseBusinessActionParamTest {

    private TwoPhaseBusinessActionParam actionParam;

    @BeforeEach
    public void setUp() {
        actionParam = new TwoPhaseBusinessActionParam();
    }

    @Test
    public void testGetActionName() {
        actionParam.setActionName("business_action");
        assertEquals("business_action", actionParam.getActionName());
    }

    @Test
    public void testIsReportDelayed() {
        actionParam.setDelayReport(true);
        assertTrue(actionParam.getDelayReport());
    }

    @Test
    public void testIsCommonFenceUsed() {
        actionParam.setUseCommonFence(true);
        assertTrue(actionParam.getUseCommonFence());
    }

    @Test
    public void testFillBusinessActionContext() {
        Map<String, Object> businessActionContextMap = new HashMap<>(2);
        businessActionContextMap.put(Constants.COMMIT_METHOD, "commit");
        businessActionContextMap.put(Constants.USE_COMMON_FENCE, false);

        actionParam.setBusinessActionContext(businessActionContextMap);

        assertEquals("commit", actionParam.getBusinessActionContext().get(Constants.COMMIT_METHOD));
        assertFalse((Boolean) actionParam.getBusinessActionContext().get(Constants.USE_COMMON_FENCE));
    }

    @Test
    public void testGetBranchType() {
        actionParam.setBranchType(BranchType.TCC);
        assertEquals(BranchType.TCC, actionParam.getBranchType());
    }
}
