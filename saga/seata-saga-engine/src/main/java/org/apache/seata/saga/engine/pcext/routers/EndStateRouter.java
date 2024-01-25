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
package org.apache.seata.saga.engine.pcext.routers;

import java.util.ArrayList;
import java.util.List;

import org.apache.seata.saga.engine.exception.EngineExecutionException;
import org.apache.seata.saga.engine.pcext.InterceptableStateRouter;
import org.apache.seata.saga.engine.pcext.StateRouter;
import org.apache.seata.saga.engine.pcext.StateRouterInterceptor;
import org.apache.seata.saga.proctrl.Instruction;
import org.apache.seata.saga.proctrl.ProcessContext;
import org.apache.seata.saga.statelang.domain.State;

/**
 * EndState Router
 *
 */
public class EndStateRouter implements StateRouter, InterceptableStateRouter {

    private List<StateRouterInterceptor> interceptors = new ArrayList<>();

    @Override
    public Instruction route(ProcessContext context, State state) throws EngineExecutionException {
        return null;//Return null to stop execution
    }

    @Override
    public List<StateRouterInterceptor> getInterceptors() {
        return interceptors;
    }

    @Override
    public void addInterceptor(StateRouterInterceptor interceptor) {
        if (interceptors != null && !interceptors.contains(interceptor)) {
            interceptors.add(interceptor);
        }
    }

    public void setInterceptors(List<StateRouterInterceptor> interceptors) {
        this.interceptors = interceptors;
    }
}
