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

import org.apache.seata.saga.statelang.domain.ExecutionStatus;
import org.apache.seata.saga.statelang.domain.StateType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class StateInstanceImplTest {

    @Test
    public void testStateInstanceImplProperties() {
        StateInstanceImpl instance = new StateInstanceImpl();
        Date now = new Date();
        Exception testException = new RuntimeException("test");
        StateInstanceImpl compensationState = new StateInstanceImpl();
        compensationState.setStatus(ExecutionStatus.SU);

        instance.setId("TEST_ID");
        instance.setMachineInstanceId("MACHINE_TEST_ID");
        instance.setName("TEST_STATE");
        instance.setType(StateType.SERVICE_TASK);
        instance.setServiceName("TestService");
        instance.setServiceMethod("testMethod");
        instance.setServiceType("SpringBean");
        instance.setBusinessKey("BUSINESS_123");
        instance.setGmtStarted(now);
        instance.setGmtUpdated(now);
        instance.setGmtEnd(now);
        instance.setForUpdate(true);
        instance.setException(testException);
        instance.setSerializedException("serialized_exception");
        instance.setInputParams("input");
        instance.setSerializedInputParams("serialized_input");
        instance.setOutputParams("output");
        instance.setSerializedOutputParams("serialized_output");
        instance.setStateIdCompensatedFor("COMPENSATE_FOR_1");
        instance.setStateIdRetriedFor("RETRY_FOR_1");
        instance.setCompensationState(compensationState);
        instance.setIgnoreStatus(true);

        assertEquals("TEST_ID", instance.getId());
        assertEquals("MACHINE_TEST_ID", instance.getMachineInstanceId());
        assertEquals("TEST_STATE", instance.getName());
        assertEquals(StateType.SERVICE_TASK, instance.getType());
        assertEquals("TestService", instance.getServiceName());
        assertEquals("testMethod", instance.getServiceMethod());
        assertEquals("SpringBean", instance.getServiceType());
        assertEquals("BUSINESS_123", instance.getBusinessKey());
        assertSame(now, instance.getGmtStarted());
        assertSame(now, instance.getGmtUpdated());
        assertSame(now, instance.getGmtEnd());
        Assertions.assertTrue(instance.isForUpdate());
        assertSame(testException, instance.getException());
        assertEquals("serialized_exception", instance.getSerializedException());
        assertEquals("input", instance.getInputParams());
        assertEquals("serialized_input", instance.getSerializedInputParams());
        assertEquals("output", instance.getOutputParams());
        assertEquals("serialized_output", instance.getSerializedOutputParams());
        assertEquals("COMPENSATE_FOR_1", instance.getStateIdCompensatedFor());
        assertEquals("RETRY_FOR_1", instance.getStateIdRetriedFor());
        assertSame(compensationState, instance.getCompensationState());
        Assertions.assertTrue(instance.isIgnoreStatus());
    }

    @Test
    public void testIsForCompensation() {
        StateInstanceImpl instance = new StateInstanceImpl();

        Assertions.assertFalse(instance.isForCompensation());

        instance.setStateIdCompensatedFor("COMPENSATE_FOR_1");
        Assertions.assertTrue(instance.isForCompensation());
    }

    @Test
    public void testGetCompensationStatus() {
        StateInstanceImpl instance = new StateInstanceImpl();

        StateInstanceImpl compensationState = new StateInstanceImpl();
        compensationState.setStatus(ExecutionStatus.FA);
        instance.setCompensationState(compensationState);
        assertEquals(ExecutionStatus.FA, instance.getCompensationStatus());
    }
}
