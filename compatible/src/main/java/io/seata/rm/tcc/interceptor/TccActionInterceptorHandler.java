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
package io.seata.rm.tcc.interceptor;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.seata.integration.tx.api.interceptor.ActionInterceptorHandler;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.apache.seata.common.Constants;
import org.apache.seata.integration.tx.api.interceptor.TwoPhaseBusinessActionParam;

/**
 * The type Tcc action interceptor handler.
 */
@Deprecated
public class TccActionInterceptorHandler extends org.apache.seata.rm.tcc.interceptor.TccActionInterceptorHandler {

    /**
     * Instantiates a new Tcc action interceptor handler.
     *
     * @param targetBean     the target bean
     * @param methodsToProxy the methods to proxy
     */
    public TccActionInterceptorHandler(Object targetBean, Set<String> methodsToProxy) {
        super(targetBean, methodsToProxy);
        actionInterceptorHandler = new ActionInterceptorHandler();
    }

    @Override
    protected Class<? extends Annotation> getAnnotationClass() {
        return TwoPhaseBusinessAction.class;
    }

    @Override
    protected boolean parserCommonFenceConfig(Annotation annotation) {
        if (annotation == null) {
            return false;
        }
        TwoPhaseBusinessAction businessAction = (TwoPhaseBusinessAction) annotation;
        return businessAction.useTCCFence();
    }

    @Override
    protected TwoPhaseBusinessActionParam createTwoPhaseBusinessActionParam(Annotation annotation) {
        TwoPhaseBusinessAction businessAction = (TwoPhaseBusinessAction) annotation;
        TwoPhaseBusinessActionParam businessActionParam = new TwoPhaseBusinessActionParam();
        businessActionParam.setActionName(businessAction.name());
        businessActionParam.setDelayReport(businessAction.isDelayReport());
        businessActionParam.setUseCommonFence(businessAction.useTCCFence());
        businessActionParam.setBranchType(getBranchType());
        Map<String, Object> businessActionContextMap = new HashMap<>(4);
        //the phase two method name
        businessActionContextMap.put(Constants.COMMIT_METHOD, businessAction.commitMethod());
        businessActionContextMap.put(Constants.ROLLBACK_METHOD, businessAction.rollbackMethod());
        businessActionContextMap.put(Constants.ACTION_NAME, businessAction.name());
        businessActionContextMap.put(Constants.USE_COMMON_FENCE, businessAction.useTCCFence());
        businessActionParam.setBusinessActionContext(businessActionContextMap);
        return businessActionParam;
    }
}
