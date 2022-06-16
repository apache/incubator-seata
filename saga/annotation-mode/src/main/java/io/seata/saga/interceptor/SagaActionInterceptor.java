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
package io.seata.saga.interceptor;

import io.seata.common.Constants;
import io.seata.common.DefaultValues;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.saga.api.SagaTransactional;
import io.seata.spring.interceptor.ActionInterceptorHandler;
import io.seata.spring.interceptor.TwoPhaseBusinessActionParam;
import io.seata.spring.remoting.RemotingDesc;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static io.seata.common.ConfigurationKeys.SAGA_ACTION_INTERCEPTOR_ORDER;
import static io.seata.common.DefaultValues.DEFAULT_DISABLE_GLOBAL_TRANSACTION;

/**
 * SAGA Interceptor
 *
 * @author ruishansun
 */
public class SagaActionInterceptor implements MethodInterceptor, ConfigurationChangeListener, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(SagaActionInterceptor.class);

    private static final int ORDER_NUM = ConfigurationFactory.getInstance().getInt(SAGA_ACTION_INTERCEPTOR_ORDER,
            DefaultValues.SAGA_ACTION_INTERCEPTOR_ORDER);

    /**
     * TODO Singleton?
     */
    private ActionInterceptorHandler actionInterceptorHandler = new ActionInterceptorHandler();

    private volatile boolean disable = ConfigurationFactory.getInstance().getBoolean(
            ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION, DEFAULT_DISABLE_GLOBAL_TRANSACTION);

    /**
     * remoting bean info
     */
    protected RemotingDesc remotingDesc;

    /**
     * Instantiates a new Saga action interceptor.
     */
    public SagaActionInterceptor() {
    }

    /**
     * Instantiates a new Saga action interceptor.
     *
     * @param remotingDesc the remoting desc
     */
    public SagaActionInterceptor(RemotingDesc remotingDesc) {
        this.remotingDesc = remotingDesc;
    }

    @Nullable
    @Override
    public Object invoke(@NotNull MethodInvocation invocation) throws Throwable {
        if (!RootContext.inGlobalTransaction() || disable || RootContext.inTccBranch()) {
            //not in transaction, or this interceptor is disabled
            return invocation.proceed();
        }
        Method method = actionInterceptorHandler.getActionInterfaceMethod(invocation, this.remotingDesc);
        SagaTransactional sagaTransactional = method.getAnnotation(SagaTransactional.class);
        //commit method
        if (sagaTransactional != null) {
            //save the xid
            String xid = RootContext.getXID();
            //save the previous branchType
            BranchType previousBranchType = RootContext.getBranchType();
            //if not SAGA, bind SAGA branchType
            if (BranchType.SAGA != previousBranchType) {
                RootContext.bindBranchType(BranchType.SAGA);
            }
            try {
                TwoPhaseBusinessActionParam businessActionParam = new TwoPhaseBusinessActionParam();
                businessActionParam.setActionName(sagaTransactional.name());
                businessActionParam.setDelayReport(false);
                businessActionParam.setUseFence(true);
                businessActionParam.setBranchType(BranchType.SAGA);
                Map<String, Object> businessActionContextMap = new HashMap<>(4);
                //the phase two method name
                businessActionContextMap.put(Constants.COMPENSATION_METHOD, sagaTransactional.compensationMethod());
                businessActionContextMap.put(Constants.ACTION_NAME, sagaTransactional.name());
                businessActionContextMap.put(Constants.USE_TCC_FENCE, true);
                businessActionParam.setBusinessActionContext(businessActionContextMap);
                //Handler the Saga Aspect, and return the business result
                return actionInterceptorHandler.proceed(method, invocation.getArguments(), xid, businessActionParam,
                        invocation::proceed);
            } finally {
                //if not SAGA, unbind branchType
                if (BranchType.SAGA != previousBranchType) {
                    RootContext.unbindBranchType();
                }
                //MDC remove branchId
                MDC.remove(RootContext.MDC_KEY_BRANCH_ID);
            }
        }

        //not SAGA commit method
        return invocation.proceed();
    }

    @Override
    public void onChangeEvent(ConfigurationChangeEvent event) {
        if (ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION.equals(event.getDataId())) {
            LOGGER.info("{} config changed, old value:{}, new value:{}", ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION,
                    disable, event.getNewValue());
            disable = Boolean.parseBoolean(event.getNewValue().trim());
        }
    }

    @Override
    public int getOrder() {
        return ORDER_NUM;
    }
}
