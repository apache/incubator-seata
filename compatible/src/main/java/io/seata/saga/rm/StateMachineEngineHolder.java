package io.seata.saga.rm;


import io.seata.saga.engine.StateMachineEngine;

public class StateMachineEngineHolder {

    private static StateMachineEngine stateMachineEngine;

    public static StateMachineEngine getStateMachineEngine() {
        return stateMachineEngine;
    }

    public static void  setStateMachineEngine(StateMachineEngine smEngine) {
        stateMachineEngine = smEngine;
    }
}
