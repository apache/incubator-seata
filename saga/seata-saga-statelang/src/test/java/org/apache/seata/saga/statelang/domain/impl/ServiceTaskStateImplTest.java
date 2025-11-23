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
package org.apache.seata.saga.statelang.domain.impl;

import org.apache.seata.saga.statelang.domain.StateType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ServiceTaskStateImplTest {

    @Test
    public void testServiceTaskStateImplProperties() throws NoSuchMethodException {
        ServiceTaskStateImpl serviceTask = new ServiceTaskStateImpl();
        List<String> paramTypes = Arrays.asList("java.lang.String", "int");
        Method testMethod = getClass().getMethod("testServiceTaskStateImplProperties");
        Map<Object, String> evaluators = new HashMap<>();
        evaluators.put("SUCCESS", "nextState");

        serviceTask.setServiceType("Dubbo");
        serviceTask.setServiceName("OrderService");
        serviceTask.setServiceMethod("createOrder");
        serviceTask.setParameterTypes(paramTypes);
        serviceTask.setMethod(testMethod);
        serviceTask.setStatusEvaluators(evaluators);
        serviceTask.setAsync(true);

        assertEquals(StateType.SERVICE_TASK, serviceTask.getType(), "ServiceTaskStateImpl should be SERVICE_TASK");
        assertEquals("Dubbo", serviceTask.getServiceType());
        assertEquals("OrderService", serviceTask.getServiceName());
        assertEquals("createOrder", serviceTask.getServiceMethod());
        assertSame(paramTypes, serviceTask.getParameterTypes());
        assertSame(testMethod, serviceTask.getMethod());
        assertSame(evaluators, serviceTask.getStatusEvaluators());
        Assertions.assertTrue(serviceTask.isAsync());
    }
}
