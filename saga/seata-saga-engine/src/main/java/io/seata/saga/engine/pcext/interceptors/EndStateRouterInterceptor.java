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
package io.seata.saga.engine.pcext.interceptors;

import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.pcext.StateRouterInterceptor;
import io.seata.saga.engine.pcext.utils.EngineUtils;
import io.seata.saga.proctrl.Instruction;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.State;

/**
 * EndStateRouter Interceptor
 *
 * @author lorne.cl
 */
public class EndStateRouterInterceptor implements StateRouterInterceptor {

    @Override
    public void preRoute(ProcessContext context, State state) throws EngineExecutionException {
        //Do Nothing
    }

    @Override
    public void postRoute(ProcessContext context, State state, Instruction instruction, Exception e)
        throws EngineExecutionException {
        EngineUtils.endStateMachine(context);
    }
}