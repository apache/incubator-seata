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
package org.apache.seata.saga.statelang.domain;

/**
 * StateType
 *
 */
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

}
