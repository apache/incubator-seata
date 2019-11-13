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

import java.util.List;

import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.pcext.InterceptibleStateRouter;
import io.seata.saga.engine.pcext.StateRouter;
import io.seata.saga.engine.pcext.StateRouterInterceptor;
import io.seata.saga.proctrl.Instruction;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.State;

/**
 * EndState Router
 *
 * @author lorne.cl
 */
public class EndStateRouter implements StateRouter, InterceptibleStateRouter {

    private List<StateRouterInterceptor> interceptors;

    @Override
    public Instruction route(ProcessContext context, State state) throws EngineExecutionException {
        return null;//Return null to stop execution
    }

    @Override
    public List<StateRouterInterceptor> getInterceptors() {
        return interceptors;
    }

    public void setInterceptors(List<StateRouterInterceptor> interceptors) {
        this.interceptors = interceptors;
    }
}