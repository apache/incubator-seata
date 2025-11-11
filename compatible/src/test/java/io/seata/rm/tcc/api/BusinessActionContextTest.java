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
package io.seata.rm.tcc.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for BusinessActionContext.
 */
public class BusinessActionContextTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                BusinessActionContext.class.isAnnotationPresent(Deprecated.class),
                "BusinessActionContext should be marked as @Deprecated");
    }

    @Test
    public void testExtendsApacheSeataBusinessActionContext() {
        assertTrue(
                org.apache.seata.rm.tcc.api.BusinessActionContext.class.isAssignableFrom(BusinessActionContext.class),
                "Should extend org.apache.seata.rm.tcc.api.BusinessActionContext");
    }

    @Test
    public void testDefaultConstructor() {
        BusinessActionContext context = new BusinessActionContext();
        assertNotNull(context);
    }

    @Test
    public void testInheritedMethods() {
        BusinessActionContext context = new BusinessActionContext();

        context.setXid("test-xid");
        context.setBranchId(123456L);
        context.setActionName("testAction");
        context.setDelayReport(true);

        assertNotNull(context);
    }
}
