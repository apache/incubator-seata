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
package org.apache.seata.saga.annotation;

import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextParameter;
import org.apache.seata.saga.rm.api.CompensationBusinessAction;

import java.util.List;

/**
 *
 */
public class NormalSagaAnnotationActionImpl implements NormalSagaAnnotationAction {

    private boolean isCommit;


    @Override
    @CompensationBusinessAction(name = "sagaActionForTest", compensationMethod = "compensation", compensationArgsClasses = {BusinessActionContext.class, SagaParam.class})
    public boolean commit(BusinessActionContext actionContext, @BusinessActionContextParameter("a") int a, @BusinessActionContextParameter(paramName = "b", index = 0) List b, @BusinessActionContextParameter(isParamInProperty = true) SagaParam sagaParam) {
        isCommit = true;
        return a > 1;
    }

    @Override
    public boolean compensation(BusinessActionContext actionContext, @BusinessActionContextParameter("sagaParam") SagaParam param) {
        isCommit = false;
        return true;
    }

    public boolean isCommit() {
        return isCommit;
    }
}