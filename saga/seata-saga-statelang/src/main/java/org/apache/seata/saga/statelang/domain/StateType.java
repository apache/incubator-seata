package org.apache.seata.saga.statelang.domain;

/**
 * StateType
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
