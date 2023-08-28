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

package io.seata.saga.engine.pcext.routers;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.pcext.InterceptableStateRouter;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.engine.pcext.StateRouter;
import io.seata.saga.engine.pcext.StateRouterInterceptor;
import io.seata.saga.engine.pcext.utils.LoopTaskUtils;
import io.seata.saga.engine.pcext.utils.ParallelContextHolder;
import io.seata.saga.proctrl.Instruction;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.impl.LoopStartStateImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Join state router
 *
 * @author ptyin
 */
public class JoinStateRouter implements StateRouter, InterceptableStateRouter {

    private final List<StateRouterInterceptor> interceptors = new ArrayList<>();

    @Override
    public Instruction route(ProcessContext context, State state) throws EngineExecutionException {
        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        ParallelContextHolder parallelContextHolder = ParallelContextHolder.getCurrent(context);
        if (parallelContextHolder != null) {
            // Route to another branch if parallel execution has not all launched, otherwise route to the next state.
            String nextStateName = parallelContextHolder.next();
            if (nextStateName == null) {
                return endBranchRouting(instruction);
            }
            if (parallelContextHolder.isFinished()) {
                context.removeVariable(DomainConstants.VAR_NAME_CURRENT_PARALLEL_CONTEXT_HOLDER);
            }

            State nextState = state.getStateMachine().getState(nextStateName);
            if (nextState == null) {
                throw new EngineExecutionException(String.format("Next state [%s] dose not exist", state.getNext()),
                        FrameworkErrorCode.ObjectNotExists);
            }
            instruction.setStateName(nextStateName);
            // Check if next state is loop task
            if (null != LoopTaskUtils.getLoopConfig(context, nextState)) {
                instruction.setTemporaryState(new LoopStartStateImpl());
            }
            return instruction;
        } else {
            return endBranchRouting(instruction);
        }
    }

    private static Instruction endBranchRouting(StateInstruction instruction) {
        instruction.setEnd(true);
        return null;
    }

    @Override
    public List<StateRouterInterceptor> getInterceptors() {
        return interceptors;
    }

    @Override
    public void addInterceptor(StateRouterInterceptor interceptor) {
        if (!interceptors.contains(interceptor)) {
            interceptors.add(interceptor);
        }
    }
}
