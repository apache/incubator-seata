/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.saga.statelang.domain;

/**
 * State Language Domain Constants
 *
 * @author lorne.cl
 */
public interface DomainConstants {

    //region State Types
    String STATE_TYPE_SERVICE_TASK = "ServiceTask";
    String STATE_TYPE_CHOICE = "Choice";
    String STATE_TYPE_FAIL = "Fail";
    String STATE_TYPE_SUCCEED = "Succeed";
    String STATE_TYPE_COMPENSATION_TRIGGER = "CompensationTrigger";
    String STATE_TYPE_SUB_STATE_MACHINE = "SubStateMachine";
    String STATE_TYPE_SUB_MACHINE_COMPENSATION = "CompensateSubMachine";
    String STATE_TYPE_SCRIPT_TASK = "ScriptTask";
    String STATE_TYPE_LOOP_START = "LoopStart";
    //endregion

    String COMPENSATE_SUB_MACHINE_STATE_NAME_PREFIX = "_compensate_sub_machine_state_";

    //region Service Types
    String SERVICE_TYPE_SPRING_BEAN = "SpringBean";
    //endregion

    //region System Variables
    String VAR_NAME_STATEMACHINE_CONTEXT = "context";
    String VAR_NAME_INPUT_PARAMS = "inputParams";
    String VAR_NAME_OUTPUT_PARAMS = "outputParams";
    String VAR_NAME_CURRENT_EXCEPTION = "currentException";//exception of current state
    String VAR_NAME_BUSINESSKEY = "_business_key_";
    String VAR_NAME_SUB_MACHINE_PARENT_ID = "_sub_machine_parent_id_";
    String VAR_NAME_CURRENT_CHOICE = "_current_choice_";
    String VAR_NAME_STATEMACHINE_ERROR_CODE = "_statemachine_error_code_";
    String VAR_NAME_STATEMACHINE_ERROR_MSG = "_statemachine_error_message_";
    String VAR_NAME_CURRENT_EXCEPTION_ROUTE = "_current_exception_route_";
    String VAR_NAME_STATEMACHINE = "_current_statemachine_";
    String VAR_NAME_STATEMACHINE_INST = "_current_statemachine_instance_";
    String VAR_NAME_STATEMACHINE_ENGINE = "_current_statemachine_engine_";
    String VAR_NAME_STATE_INST = "_current_state_instance_";
    String VAR_NAME_STATEMACHINE_CONFIG = "_statemachine_config_";
    String VAR_NAME_FAIL_END_STATE_FLAG = "_fail_end_state_flag_";
    String VAR_NAME_CURRENT_COMPENSATION_HOLDER = "_current_compensation_holder_";
    String VAR_NAME_RETRIED_STATE_INST_ID = "_retried_state_instance_id";
    String VAR_NAME_OPERATION_NAME = "_operation_name_";
    String VAR_NAME_ASYNC_CALLBACK = "_async_callback_";
    String VAR_NAME_CURRENT_COMPEN_TRIGGER_STATE = "_is_compensating_";
    String VAR_NAME_IS_EXCEPTION_NOT_CATCH = "_is_exception_not_catch_";
    String VAR_NAME_PARENT_ID = "_parent_id_";
    String VAR_NAME_SUB_STATEMACHINE_EXEC_STATUE = "_sub_statemachine_execution_status_";
    String VAR_NAME_IS_FOR_SUB_STATMACHINE_FORWARD = "_is_for_sub_statemachine_forward_";
    String VAR_NAME_FIRST_COMPENSATION_STATE_STARTED = "_first_compensation_state_started";
    String VAR_NAME_GLOBAL_TX = "_global_transaction_";
    String VAR_NAME_IS_ASYNC_EXECUTION = "_is_async_execution_";
    String VAR_NAME_IS_LOOP_STATE = "_is_loop_state_";
    String VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER = "_current_loop_context_holder_";
    //endregion

    // region of loop
    String LOOP_COUNTER = "loopCounter";
    String LOOP_SEMAPHORE = "loopSemaphore";
    String LOOP_RESULT = "loopResult";
    String NUMBER_OF_INSTANCES = "nrOfInstances";
    String NUMBER_OF_ACTIVE_INSTANCES = "nrOfActiveInstances";
    String NUMBER_OF_COMPLETED_INSTANCES = "nrOfCompletedInstances";
    // endregion

    String OPERATION_NAME_START = "start";
    String OPERATION_NAME_FORWARD = "forward";
    String OPERATION_NAME_COMPENSATE = "compensate";

    String SEQ_ENTITY_STATE_MACHINE = "STATE_MACHINE";
    String SEQ_ENTITY_STATE_MACHINE_INST = "STATE_MACHINE_INST";
    String SEQ_ENTITY_STATE_INST = "STATE_INST";

    String EXPRESSION_TYPE_SEQUENCE = "Sequence";
    String EVALUATOR_TYPE_EXCEPTION = "Exception";

    String SEPERATOR_PARENT_ID      = ":";

    String DEFAULT_JSON_PARSER      = "fastjson";
}