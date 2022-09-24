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
package io.seata.rm.tcc.interceptor;

import io.seata.common.Constants;
import io.seata.common.DefaultValues;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.commonapi.interceptor.ActionInterceptorHandler;
import io.seata.commonapi.interceptor.TwoPhaseBusinessActionParam;
import io.seata.commonapi.remoting.RemotingDesc;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static io.seata.common.DefaultValues.DEFAULT_DISABLE_GLOBAL_TRANSACTION;
import static io.seata.core.constants.ConfigurationKeys.TCC_ACTION_INTERCEPTOR_ORDER;

/**
 * TCC Interceptor
 *
 * @author zhangsen
 * @author wang.liang
 */
public class TccActionInterceptor implements MethodInterceptor, ConfigurationChangeListener, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(TccActionInterceptor.class);
    private static final int ORDER_NUM = ConfigurationFactory.getInstance().getInt(TCC_ACTION_INTERCEPTOR_ORDER,
            DefaultValues.TCC_ACTION_INTERCEPTOR_ORDER);

    private ActionInterceptorHandler actionInterceptorHandler = new ActionInterceptorHandler();

    private volatile boolean disable = ConfigurationFactory.getInstance().getBoolean(
            ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION, DEFAULT_DISABLE_GLOBAL_TRANSACTION);

    /**
     * remoting bean info
     */
    protected RemotingDesc remotingDesc;

    /**
     * Instantiates a new Tcc action interceptor.
     */
    public TccActionInterceptor() {
    }

    /**
     * Instantiates a new Tcc action interceptor.
     *
     * @param remotingDesc the remoting desc
     */
    public TccActionInterceptor(RemotingDesc remotingDesc) {
        this.remotingDesc = remotingDesc;
    }

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        if (!RootContext.inGlobalTransaction() || disable || RootContext.inSagaBranch()) {
            //not in transaction, or this interceptor is disabled
            return invocation.proceed();
        }
        Method method = actionInterceptorHandler.getActionInterfaceMethod(invocation, this.remotingDesc);
        TwoPhaseBusinessAction businessAction = method.getAnnotation(TwoPhaseBusinessAction.class);
        //try method
        if (businessAction != null) {
            //save the xid
            String xid = RootContext.getXID();
            //save the previous branchType
            BranchType previousBranchType = RootContext.getBranchType();
            //if not TCC, bind TCC branchType
            if (BranchType.TCC != previousBranchType) {
                RootContext.bindBranchType(BranchType.TCC);
            }
            try {
                TwoPhaseBusinessActionParam businessActionParam = new TwoPhaseBusinessActionParam();
                businessActionParam.setActionName(businessAction.name());
                businessActionParam.setDelayReport(businessAction.isDelayReport());
                businessActionParam.setUseCommonFence(businessAction.useTCCFence());
                businessActionParam.setBranchType(BranchType.TCC);
                Map<String, Object> businessActionContextMap = new HashMap<>(4);
                //the phase two method name
                businessActionContextMap.put(Constants.COMMIT_METHOD, businessAction.commitMethod());
                businessActionContextMap.put(Constants.ROLLBACK_METHOD, businessAction.rollbackMethod());
                businessActionContextMap.put(Constants.ACTION_NAME, businessAction.name());
                businessActionContextMap.put(Constants.USE_COMMON_FENCE, businessAction.useTCCFence());
                businessActionParam.setBusinessActionContext(businessActionContextMap);
                //Handler the TCC Aspect, and return the business result
                return actionInterceptorHandler.proceed(method, invocation.getArguments(), xid, businessActionParam,
                        invocation::proceed);
            } finally {
                //if not TCC, unbind branchType
                if (BranchType.TCC != previousBranchType) {
                    RootContext.unbindBranchType();
                }
                //MDC remove branchId
                MDC.remove(RootContext.MDC_KEY_BRANCH_ID);
            }
        }

        //not TCC try method
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