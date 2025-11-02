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
package io.seata.saga.statelang.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for DomainConstants interface compatibility wrapper.
 */
public class DomainConstantsTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                DomainConstants.class.isAnnotationPresent(Deprecated.class),
                "DomainConstants should be marked as @Deprecated");
    }

    @Test
    public void testIsInterface() {
        assertTrue(DomainConstants.class.isInterface(), "DomainConstants should be an interface");
    }

    @Test
    public void testStateTypeConstants() {
        assertEquals("ServiceTask", DomainConstants.STATE_TYPE_SERVICE_TASK);
        assertEquals("Choice", DomainConstants.STATE_TYPE_CHOICE);
        assertEquals("Fail", DomainConstants.STATE_TYPE_FAIL);
        assertEquals("Succeed", DomainConstants.STATE_TYPE_SUCCEED);
        assertEquals("CompensationTrigger", DomainConstants.STATE_TYPE_COMPENSATION_TRIGGER);
        assertEquals("SubStateMachine", DomainConstants.STATE_TYPE_SUB_STATE_MACHINE);
        assertEquals("CompensateSubMachine", DomainConstants.STATE_TYPE_SUB_MACHINE_COMPENSATION);
        assertEquals("ScriptTask", DomainConstants.STATE_TYPE_SCRIPT_TASK);
        assertEquals("LoopStart", DomainConstants.STATE_TYPE_LOOP_START);
    }

    @Test
    public void testCompensateSubMachineStateNamePrefix() {
        assertEquals("_compensate_sub_machine_state_", DomainConstants.COMPENSATE_SUB_MACHINE_STATE_NAME_PREFIX);
    }

    @Test
    public void testServiceTypeConstants() {
        assertEquals("SpringBean", DomainConstants.SERVICE_TYPE_SPRING_BEAN);
    }

    @Test
    public void testSystemVariableConstants() {
        assertEquals("context", DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT);
        assertEquals("inputParams", DomainConstants.VAR_NAME_INPUT_PARAMS);
        assertEquals("outputParams", DomainConstants.VAR_NAME_OUTPUT_PARAMS);
        assertEquals("currentException", DomainConstants.VAR_NAME_CURRENT_EXCEPTION);
        assertEquals("_business_key_", DomainConstants.VAR_NAME_BUSINESSKEY);
        assertEquals("_sub_machine_parent_id_", DomainConstants.VAR_NAME_SUB_MACHINE_PARENT_ID);
        assertEquals("_current_choice_", DomainConstants.VAR_NAME_CURRENT_CHOICE);
        assertEquals("_statemachine_error_code_", DomainConstants.VAR_NAME_STATEMACHINE_ERROR_CODE);
        assertEquals("_statemachine_error_message_", DomainConstants.VAR_NAME_STATEMACHINE_ERROR_MSG);
        assertEquals("_current_exception_route_", DomainConstants.VAR_NAME_CURRENT_EXCEPTION_ROUTE);
        assertEquals("_current_statemachine_", DomainConstants.VAR_NAME_STATEMACHINE);
        assertEquals("_current_statemachine_instance_", DomainConstants.VAR_NAME_STATEMACHINE_INST);
        assertEquals("_current_statemachine_engine_", DomainConstants.VAR_NAME_STATEMACHINE_ENGINE);
        assertEquals("_current_state_instance_", DomainConstants.VAR_NAME_STATE_INST);
        assertEquals("_statemachine_config_", DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);
        assertEquals("_fail_end_state_flag_", DomainConstants.VAR_NAME_FAIL_END_STATE_FLAG);
        assertEquals("_current_compensation_holder_", DomainConstants.VAR_NAME_CURRENT_COMPENSATION_HOLDER);
        assertEquals("_retried_state_instance_id", DomainConstants.VAR_NAME_RETRIED_STATE_INST_ID);
        assertEquals("_operation_name_", DomainConstants.VAR_NAME_OPERATION_NAME);
        assertEquals("_async_callback_", DomainConstants.VAR_NAME_ASYNC_CALLBACK);
        assertEquals("_is_compensating_", DomainConstants.VAR_NAME_CURRENT_COMPEN_TRIGGER_STATE);
        assertEquals("_is_exception_not_catch_", DomainConstants.VAR_NAME_IS_EXCEPTION_NOT_CATCH);
        assertEquals("_parent_id_", DomainConstants.VAR_NAME_PARENT_ID);
        assertEquals("_sub_statemachine_execution_status_", DomainConstants.VAR_NAME_SUB_STATEMACHINE_EXEC_STATUE);
        assertEquals("_is_for_sub_statemachine_forward_", DomainConstants.VAR_NAME_IS_FOR_SUB_STATMACHINE_FORWARD);
        assertEquals("_first_compensation_state_started", DomainConstants.VAR_NAME_FIRST_COMPENSATION_STATE_STARTED);
        assertEquals("_global_transaction_", DomainConstants.VAR_NAME_GLOBAL_TX);
        assertEquals("_is_async_execution_", DomainConstants.VAR_NAME_IS_ASYNC_EXECUTION);
        assertEquals("_is_loop_state_", DomainConstants.VAR_NAME_IS_LOOP_STATE);
        assertEquals("_current_loop_context_holder_", DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER);
    }

    @Test
    public void testLoopConstants() {
        assertEquals("loopCounter", DomainConstants.LOOP_COUNTER);
        assertEquals("loopSemaphore", DomainConstants.LOOP_SEMAPHORE);
        assertEquals("loopResult", DomainConstants.LOOP_RESULT);
        assertEquals("nrOfInstances", DomainConstants.NUMBER_OF_INSTANCES);
        assertEquals("nrOfActiveInstances", DomainConstants.NUMBER_OF_ACTIVE_INSTANCES);
        assertEquals("nrOfCompletedInstances", DomainConstants.NUMBER_OF_COMPLETED_INSTANCES);
    }

    @Test
    public void testOperationNameConstants() {
        assertEquals("start", DomainConstants.OPERATION_NAME_START);
        assertEquals("forward", DomainConstants.OPERATION_NAME_FORWARD);
        assertEquals("compensate", DomainConstants.OPERATION_NAME_COMPENSATE);
    }

    @Test
    public void testSequenceEntityConstants() {
        assertEquals("STATE_MACHINE", DomainConstants.SEQ_ENTITY_STATE_MACHINE);
        assertEquals("STATE_MACHINE_INST", DomainConstants.SEQ_ENTITY_STATE_MACHINE_INST);
        assertEquals("STATE_INST", DomainConstants.SEQ_ENTITY_STATE_INST);
    }

    @Test
    public void testExpressionTypeConstants() {
        assertEquals("Sequence", DomainConstants.EXPRESSION_TYPE_SEQUENCE);
        assertEquals("Exception", DomainConstants.EXPRESSION_TYPE_EXCEPTION);
    }

    @Test
    public void testOtherConstants() {
        assertEquals(":", DomainConstants.SEPERATOR_PARENT_ID);
        assertEquals("fastjson", DomainConstants.DEFAULT_JSON_PARSER);
    }
}
