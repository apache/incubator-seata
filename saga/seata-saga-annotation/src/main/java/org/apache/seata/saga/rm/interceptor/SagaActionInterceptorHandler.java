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
package org.apache.seata.saga.rm.interceptor;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.seata.common.Constants;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.integration.tx.api.interceptor.TwoPhaseBusinessActionParam;
import org.apache.seata.rm.tcc.interceptor.TccActionInterceptorHandler;
import org.apache.seata.saga.rm.api.CompensationBusinessAction;

/**
 * saga-annotation invocationHandler, extended from TccActionInterceptorHandler.
 */
public class SagaActionInterceptorHandler extends TccActionInterceptorHandler {


    public SagaActionInterceptorHandler(Object target, Set<String> methodsToProxy) {
        super(target, methodsToProxy);
    }

    @Override
    protected TwoPhaseBusinessActionParam createTwoPhaseBusinessActionParam(Annotation annotation) {
        CompensationBusinessAction businessAction = (CompensationBusinessAction) annotation;

        TwoPhaseBusinessActionParam businessActionParam = new TwoPhaseBusinessActionParam();
        businessActionParam.setActionName(businessAction.name());
        businessActionParam.setDelayReport(businessAction.isDelayReport());
        businessActionParam.setUseCommonFence(businessAction.useFence());
        businessActionParam.setBranchType(BranchType.SAGA_ANNOTATION);

        Map<String, Object> businessActionContextMap = new HashMap<>(4);
        businessActionContextMap.put(Constants.ROLLBACK_METHOD, businessAction.compensationMethod());
        businessActionContextMap.put(Constants.ACTION_NAME, businessAction.name());
        businessActionContextMap.put(Constants.USE_COMMON_FENCE, businessAction.useFence());
        businessActionParam.setBusinessActionContext(businessActionContextMap);

        return businessActionParam;
    }

    @Override
    protected BranchType getBranchType() {
        return BranchType.SAGA_ANNOTATION;
    }

    @Override
    protected Class<? extends Annotation> getAnnotationClass() {
        return CompensationBusinessAction.class;
    }
}
