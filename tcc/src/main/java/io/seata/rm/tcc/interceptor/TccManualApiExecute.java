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

import io.seata.commonapi.autoproxy.ManualApiExecute;
import io.seata.common.Constants;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.commonapi.interceptor.ActionInterceptorHandler;
import io.seata.commonapi.interceptor.TwoPhaseBusinessActionParam;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class TccManualApiExecute implements ManualApiExecute {

    private ActionInterceptorHandler actionInterceptorHandler;
    
    @Override
    public void manualApiBefore(Method method,  Object[] arguments) throws Throwable {

        TwoPhaseBusinessAction businessAction = method.getAnnotation(TwoPhaseBusinessAction.class);
        if (businessAction != null) {
            //save the xid
            String xid = RootContext.getXID();
            //save the previous branchType
            BranchType previousBranchType = RootContext.getBranchType();
            //if not TCC, bind TCC branchType
            if (BranchType.TCC != previousBranchType) {
                RootContext.bindBranchType(BranchType.TCC);
            }

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
            if (actionInterceptorHandler == null) {
                actionInterceptorHandler = new ActionInterceptorHandler();
            }
            actionInterceptorHandler.proceedManual(method, arguments, xid, businessActionParam);
        }
    }
}
