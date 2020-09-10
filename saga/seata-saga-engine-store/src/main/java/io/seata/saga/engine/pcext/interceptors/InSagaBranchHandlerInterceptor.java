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

import io.seata.common.loader.LoadLevel;
import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.pcext.InterceptableStateHandler;
import io.seata.saga.engine.pcext.StateHandlerInterceptor;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.tm.api.GlobalTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * InSagaBranchHandler Interceptor
 *
 * @author wang.liang
 */
@LoadLevel(name = "InSagaBranch", order = 50)
public class InSagaBranchHandlerInterceptor implements StateHandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(InSagaBranchHandlerInterceptor.class);

    @Override
    public boolean match(Class<? extends InterceptableStateHandler> clazz) {
        // This interceptor will intercept all types of InterceptableStateHandler
        return clazz != null;
    }

    @Override
    public void preProcess(ProcessContext context) throws EngineExecutionException {
        // get xid
        String xid = this.getXidFromProcessContext(context);
        if (StringUtils.isBlank(xid)) {
            LOGGER.warn("There is no xid in the process context.");
            return;
        }

        // logger.warn if previousXid is not equals to xid
        if (LOGGER.isWarnEnabled()) {
            String previousXid = RootContext.getXID();
            if (previousXid != null) {
                if (!StringUtils.equalsIgnoreCase(previousXid, xid)) {
                    LOGGER.warn("xid in change from {} to {}, Please don't use state machine engine in other global transaction.",
                        previousXid, xid);
                }
            }
        }

        // bind xid and branchType
        RootContext.bind(xid);
        RootContext.bindBranchType(BranchType.SAGA);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[{}] Begin process the state instance in the saga branch.", xid);
        }
    }

    @Override
    public void postProcess(ProcessContext context, Exception exp) throws EngineExecutionException {
        // unbind xid and branchType
        String xid = RootContext.unbind();
        RootContext.unbindBranchType();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[{}] Finish process the state instance in the saga branch.", xid);
        }
    }

    /**
     * Gets xid from saga process context.
     *
     * @return the xid
     */
    protected String getXidFromProcessContext(ProcessContext context) {
        String xid = null;
        Map<String, Object> contextVariable = (Map<String, Object>) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT);
        if (contextVariable != null && contextVariable.containsKey(DomainConstants.VAR_NAME_GLOBAL_TX)) {
            GlobalTransaction globalTransaction = (GlobalTransaction) contextVariable.get(DomainConstants.VAR_NAME_GLOBAL_TX);
            xid = globalTransaction.getXid();
        } else {
            StateMachineInstance stateMachineInstance = (StateMachineInstance) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_INST);
            if (stateMachineInstance != null) {
                xid = stateMachineInstance.getId();
            }
        }
        return xid;
    }
}
