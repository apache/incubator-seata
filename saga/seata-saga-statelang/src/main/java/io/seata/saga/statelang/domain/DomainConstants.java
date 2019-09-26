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
public class DomainConstants {

    /** State Types **/
    public static final String STATE_TYPE_SERVICE_TASK                  = "ServiceTask";
    public static final String STATE_TYPE_CHOICE                        = "Choice";
    public static final String STATE_TYPE_FAIL                          = "Fail";
    public static final String STATE_TYPE_SUCCEED                       = "Succeed";
    public static final String STATE_TYPE_COMPENSATION_TRIGGER          = "CompensationTrigger";
    public static final String STATE_TYPE_SUB_STATE_MACHINE             = "SubStateMachine";
    public static final String STATE_TYPE_SUB_MACHINE_COMPENSATION      = "CompensateSubMachine";
    /** State Types **/

    public static final String COMPENSATE_SUB_MACHINE_STATE_NAME_PREFIX = "_compensate_sub_machine_state_";


    /** Service Types **/
    public static final String SERVICE_TYPE_SPRING_BEAN = "SpringBean";
    /** Service Types **/

    /** System Variables **/
    public static final String VAR_NAME_STATEMACHINE_CONTEXT             = "context";
    public static final String VAR_NAME_INPUT_PARAMS                     = "inputParams";
    public static final String VAR_NAME_OUTPUT_PARAMS                    = "outputParams";
    public static final String VAR_NAME_CURRENT_EXCEPTION                = "currentException";//exception of current state
    public static final String VAR_NAME_BUSINESSKEY                      = "_business_key_";
    public static final String VAR_NAME_SUB_MACHINE_PARENT_ID            = "_sub_machine_parent_id_";
    public static final String VAR_NAME_CURRENT_CHOICE                   = "_current_choice_";
    public static final String VAR_NAME_STATEMACHINE_ERROR_CODE          = "_statemachine_error_code_";
    public static final String VAR_NAME_STATEMACHINE_ERROR_MSG           = "_statemachine_error_message_";
    public static final String VAR_NAME_CURRENT_EXCEPTION_ROUTE          = "_current_exception_route_";
    public static final String VAR_NAME_STATEMACHINE                     = "_current_statemachine_";
    public static final String VAR_NAME_STATEMACHINE_INST                = "_current_statemachine_instance_";
    public static final String VAR_NAME_STATEMACHINE_ENGINE              = "_current_statemachine_engine_";
    public static final String VAR_NAME_STATE_INST                       = "_current_state_instance_";
    public static final String VAR_NAME_STATEMACHINE_CONFIG              = "_statemachine_config_";
    public static final String VAR_NAME_FAIL_END_STATE_FLAG              = "_fail_end_state_flag_";
    public static final String VAR_NAME_CURRENT_COMPENSATION_HOLDER      = "_current_compensation_holder_";
    public static final String VAR_NAME_RETRIED_STATE_INST_ID            = "_retried_state_instance_id";
    public static final String VAR_NAME_OPERATION_NAME                   = "_operation_name_";
    public static final String VAR_NAME_ASYNC_CALLBACK                   = "_async_callback_";
    public static final String VAR_NAME_CURRENT_COMPEN_TRIGGER_STATE     = "_is_compensating_";
    public static final String VAR_NAME_IS_EXCEPTION_NOT_CATCH           = "_is_exception_not_catch_";
    public static final String VAR_NAME_PARENT_ID                        = "_parent_id_";
    public static final String VAR_NAME_SUB_STATEMACHINE_EXEC_STATUE     = "_sub_statemachine_execution_status_";
    public static final String VAR_NAME_IS_FOR_SUB_STATMACHINE_FORWARD   = "_is_for_sub_statemachine_forward_";
    public static final String VAR_NAME_FIRST_COMPENSATION_STATE_STARTED = "_first_compensation_state_started";
    public static final String VAR_NAME_GLOBAL_TX                        = "_global_transaction_";
    public static final String VAR_NAME_ROOT_CONTEXT_HOLDER              = "_root_context_holder_";

    public static final String OPERATION_NAME_START      = "start";
    public static final String OPERATION_NAME_FORWARD    = "forward";
    public static final String OPERATION_NAME_COMPENSATE = "compensate";

    public static final String SEQ_ENTITY_STATE_MACHINE      = "STATE_MACHINE";
    public static final String SEQ_ENTITY_STATE_MACHINE_INST = "STATE_MACHINE_INST";
    public static final String SEQ_ENTITY_STATE_INST         = "STATE_INST";

    public static final String EXPRESSION_TYPE_SEQUENCE = "Sequence";
    public static final String EVALUATOR_TYPE_EXCEPTION = "Exception";

}