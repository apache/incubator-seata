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

/**
 * StateType
 */
@Deprecated
public enum StateType {

    /**
     * ServiceTask State
     */
    SERVICE_TASK("ServiceTask"),

    /**
     * Choice State
     */
    CHOICE("Choice"),

    /**
     * Fail State
     */
    FAIL("Fail"),

    /**
     * Succeed State
     */
    SUCCEED("Succeed"),

    /**
     * CompensationTrigger State
     */
    COMPENSATION_TRIGGER("CompensationTrigger"),

    /**
     * SubStateMachine State
     */
    SUB_STATE_MACHINE("SubStateMachine"),

    /**
     * CompensateSubMachine State
     */
    SUB_MACHINE_COMPENSATION("CompensateSubMachine"),

    /**
     * ScriptTask State
     */
    SCRIPT_TASK("ScriptTask"),

    /**
     * LoopStart State
     */
    LOOP_START("LoopStart");


    private String value;

    StateType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static StateType getStateType(String value) {
        for (StateType stateType : values()) {
            if (stateType.getValue().equalsIgnoreCase(value)) {
                return stateType;
            }
        }

        throw new IllegalArgumentException("Unknown StateType[" + value + "]");
    }


    public static StateType wrap(org.apache.seata.saga.statelang.domain.StateType target) {
        if (target == null) {
            return null;
        }
        switch (target) {
            case SERVICE_TASK:
                return SERVICE_TASK;
            case CHOICE:
                return CHOICE;
            case FAIL:
                return FAIL;
            case SUCCEED:
                return SUCCEED;
            case COMPENSATION_TRIGGER:
                return COMPENSATION_TRIGGER;
            case SUB_STATE_MACHINE:
                return SUB_STATE_MACHINE;
            case SUB_MACHINE_COMPENSATION:
                return SUB_MACHINE_COMPENSATION;
            case SCRIPT_TASK:
                return SCRIPT_TASK;
            case LOOP_START:
                return LOOP_START;
            default:
                throw new IllegalArgumentException("Cannot convert " + target.name());
        }
    }

    public org.apache.seata.saga.statelang.domain.StateType unwrap() {
        switch (this) {
            case SERVICE_TASK:
                return org.apache.seata.saga.statelang.domain.StateType.SERVICE_TASK;
            case CHOICE:
                return org.apache.seata.saga.statelang.domain.StateType.CHOICE;
            case FAIL:
                return org.apache.seata.saga.statelang.domain.StateType.FAIL;
            case SUCCEED:
                return org.apache.seata.saga.statelang.domain.StateType.SUCCEED;
            case COMPENSATION_TRIGGER:
                return org.apache.seata.saga.statelang.domain.StateType.COMPENSATION_TRIGGER;
            case SUB_STATE_MACHINE:
                return org.apache.seata.saga.statelang.domain.StateType.SUB_STATE_MACHINE;
            case SUB_MACHINE_COMPENSATION:
                return org.apache.seata.saga.statelang.domain.StateType.SUB_MACHINE_COMPENSATION;
            case SCRIPT_TASK:
                return org.apache.seata.saga.statelang.domain.StateType.SCRIPT_TASK;
            case LOOP_START:
                return org.apache.seata.saga.statelang.domain.StateType.LOOP_START;
            default:
                throw new IllegalArgumentException("Cannot convert " + this.name());
        }
    }

}
