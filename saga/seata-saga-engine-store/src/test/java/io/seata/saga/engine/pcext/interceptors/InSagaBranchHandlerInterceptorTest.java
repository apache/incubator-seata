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

import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.saga.engine.mock.MockGlobalTransaction;
import io.seata.saga.engine.pcext.StateHandlerInterceptor;
import io.seata.saga.engine.pcext.handlers.ScriptTaskStateHandler;
import io.seata.saga.engine.pcext.handlers.ServiceTaskStateHandler;
import io.seata.saga.engine.pcext.handlers.SubStateMachineHandler;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.impl.ProcessContextImpl;
import io.seata.saga.statelang.domain.DomainConstants;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * In Saga Branch Handler Interceptor Test
 *
 * @author wang.liang
 */
public class InSagaBranchHandlerInterceptorTest {
    private static final String DEFAULT_XID = "0123456789";

    @Test
    public void test_match() {
        StateHandlerInterceptor interceptor = new InSagaBranchHandlerInterceptor();
        assertThat(interceptor.match(null)).isFalse();
        assertThat(interceptor.match(ServiceTaskStateHandler.class)).isTrue();
        assertThat(interceptor.match(SubStateMachineHandler.class)).isTrue();
        assertThat(interceptor.match(ScriptTaskStateHandler.class)).isTrue();
    }

    @Test
    public void test_preProcess_postProcess() {
        StateHandlerInterceptor interceptor = new InSagaBranchHandlerInterceptor();
        ProcessContext context = this.buildContext();

        assertThat(RootContext.getXID()).isNull();
        assertThat(RootContext.getBranchType()).isNull();

        interceptor.preProcess(context);
        assertThat(RootContext.getXID()).isEqualTo(DEFAULT_XID);
        assertThat(RootContext.getBranchType()).isEqualTo(BranchType.SAGA.name());

        interceptor.postProcess(context, null);
        assertThat(RootContext.getXID()).isNull();
        assertThat(RootContext.getBranchType()).isNull();
    }

    /**
     * Build context.
     *
     * @return the context
     */
    private ProcessContext buildContext() {
        ProcessContext processContext = new ProcessContextImpl();

        // mock context variable
        Map<String, Object> contextVariable = new HashMap<>();
        contextVariable.put(DomainConstants.VAR_NAME_GLOBAL_TX, new MockGlobalTransaction(DEFAULT_XID));

        processContext.setVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT, contextVariable);

        return processContext;
    }
}
