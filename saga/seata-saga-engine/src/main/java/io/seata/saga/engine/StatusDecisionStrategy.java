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
package io.seata.saga.engine;

import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.StateMachineInstance;

/**
 * Default state machine execution status decision strategy.
 * The strategy is to traverse the execution state of each state executed.
 * If all state are successfully executed the state machine is successfully executed,
 * if there is a state that fails to execute which is for data update, the state machine execution status is considered to be UN (the data is inconsistent),
 * otherwise FA (failure: no data inconsistency)
 *
 * @author lorne.cl
 */
public interface StatusDecisionStrategy {

    /**
     * Determine state machine execution status when executing to EndState
     * @param context
     * @param stateMachineInstance
     * @param exp
     */
    void decideOnEndState(ProcessContext context, StateMachineInstance stateMachineInstance, Exception exp);

    /**
     * Determine state machine execution status when executing TaskState error
     * @param context
     * @param stateMachineInstance
     * @param exp
     */
    void decideOnTaskStateFail(ProcessContext context, StateMachineInstance stateMachineInstance, Exception exp);

    /**
     * Determine the forward execution state of the state machine
     * @param stateMachineInstance
     * @param exp
     * @param specialPolicy
     * @return
     */
    boolean decideMachineForwardExecutionStatus(StateMachineInstance stateMachineInstance, Exception exp,
        boolean specialPolicy);
}