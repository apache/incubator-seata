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
import io.seata.common.util.StringUtils;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.pcext.InterceptableStateRouter;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.engine.pcext.StateRouter;
import io.seata.saga.engine.pcext.StateRouterInterceptor;
import io.seata.saga.engine.pcext.utils.LoopTaskUtils;
import io.seata.saga.proctrl.Instruction;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.impl.ForkStateImpl;
import io.seata.saga.statelang.domain.impl.LoopStartStateImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Fork state router
 *
 * @author ptyin
 */
public class ForkStateRouter implements StateRouter, InterceptableStateRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForkStateRouter.class);

    private final List<StateRouterInterceptor> interceptors = new ArrayList<>();

    @Override
    public Instruction route(ProcessContext context, State state) throws EngineExecutionException {
        StateInstruction stateInstruction = context.getInstruction(StateInstruction.class);

        String pairedJoinStateName = ((ForkStateImpl) state).getPairedJoinState();
        State pairedJoinState = state.getStateMachine().getState(pairedJoinStateName);
        if (pairedJoinState == null) {
            throw new EngineExecutionException(String.format("No paired join state for fork state [%s]",
                    pairedJoinStateName), FrameworkErrorCode.ObjectNotExists);
        }
        String stateNameAfterJoin = pairedJoinState.getNext();
        if (StringUtils.isBlank(stateNameAfterJoin)) {
            LOGGER.debug("No subsequent state for join state {}", pairedJoinState.getName());
            return null;
        }
        State stateAfterJoin = state.getStateMachine().getState(stateNameAfterJoin);
        if (stateAfterJoin == null) {
            throw new EngineExecutionException(String.format("Next state [%s] is not exits", stateNameAfterJoin),
                    FrameworkErrorCode.ObjectNotExists);
        }
        stateInstruction.setStateName(stateNameAfterJoin);

        // Check if next state is loop task
        if (null != LoopTaskUtils.getLoopConfig(context, stateAfterJoin)) {
            stateInstruction.setTemporaryState(new LoopStartStateImpl());
        }

        return stateInstruction;
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
