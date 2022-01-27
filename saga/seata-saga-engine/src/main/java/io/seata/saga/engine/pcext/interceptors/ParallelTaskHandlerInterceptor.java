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

import java.util.concurrent.Semaphore;

import io.seata.common.loader.LoadLevel;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.pcext.InterceptableStateHandler;
import io.seata.saga.engine.pcext.StateHandlerInterceptor;
import io.seata.saga.engine.pcext.handlers.ServiceTaskStateHandler;
import io.seata.saga.engine.pcext.handlers.SubStateMachineHandler;
import io.seata.saga.engine.pcext.utils.ParallelContextHolder;
import io.seata.saga.proctrl.HierarchicalProcessContext;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author anselleeyy
 */
@LoadLevel(name = "ParallelTask", order = 90)
public class ParallelTaskHandlerInterceptor implements StateHandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelTaskHandlerInterceptor.class);

    @Override
    public boolean match(Class<? extends InterceptableStateHandler> clazz) {
        return clazz != null &&
            (ServiceTaskStateHandler.class.isAssignableFrom(clazz)
                || SubStateMachineHandler.class.isAssignableFrom(clazz)
                || ScriptTaskHandlerInterceptor.class.isAssignableFrom(clazz));
    }

    @Override
    public void preProcess(ProcessContext context) throws EngineExecutionException {

    }

    @Override
    public void postProcess(ProcessContext context, Exception e) throws EngineExecutionException {

        if (context.hasVariable(DomainConstants.VAR_NAME_IS_PARALLEL_STATE)) {

            Exception exp =
                (Exception) ((HierarchicalProcessContext) context).getVariableLocally(DomainConstants.VAR_NAME_CURRENT_EXCEPTION);

            if (e != null && context.hasVariable(DomainConstants.PARALLEL_SEMAPHORE)) {
                Semaphore semaphore = (Semaphore) context.getVariable(DomainConstants.PARALLEL_SEMAPHORE);
                semaphore.release();
            }

            if (exp != null || e != null) {
                ParallelContextHolder.getCurrent(context, true).setFailEnd(true);
            }
        }
    }

}
