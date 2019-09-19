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
package io.seata.spring.tcc;

import io.seata.common.executor.Callback;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import io.seata.rm.tcc.interceptor.ActionInterceptorHandler;
import io.seata.spring.api.SimpleInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * SAGA Interceptor
 *
 * @author zhangsen
 */
public class TccSimpleInterceptor implements SimpleInterceptor {
    
    private static final Logger      LOGGER                   = LoggerFactory.getLogger(TccSimpleInterceptor.class);
    
    private ActionInterceptorHandler actionInterceptorHandler = new ActionInterceptorHandler();
    
    public void invoke(Object proxy, Method method, Object[] methodArgs) throws Throwable {
        if (!RootContext.inGlobalTransaction()) {
            //not in transaction
            return;
        }
        TwoPhaseBusinessAction businessAction = method.getAnnotation(TwoPhaseBusinessAction.class);
        //try method
        if (businessAction != null) {
            //save the xid
            String xid = RootContext.getXID();
            String xidType = String.format("%s_%s", xid, BranchType.SAGA.name());
            //clear the context
            RootContext.unbind();
            RootContext.bindType(xidType);
            try {
                actionInterceptorHandler.proceed(method, methodArgs, xid, businessAction, new Callback<Object>() {
                    @Override
                    public Object execute() throws Throwable {
                        return null;
                    }
                });
            } finally {
                //recovery the context
                RootContext.bind(xid);
                RootContext.unbindType();
            }
        }
    }
    
}
