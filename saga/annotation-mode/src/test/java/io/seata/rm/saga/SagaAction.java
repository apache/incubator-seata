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
package io.seata.rm.saga;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.saga.api.SagaTransactional;
import io.seata.spring.annotation.LocalService;

/**
 * The interface Saga action.
 *
 * @author ruishansun
 */
@LocalService
public interface SagaAction {

    /**
     * Commit boolean.
     *
     * @param actionContext the action context
     * @param a             the a
     * @return the boolean
     */
    @SagaTransactional(name = "sagaActionForTest", compensationMethod = "compensation")
    boolean commit(BusinessActionContext actionContext,
                   @BusinessActionContextParameter("a") int a);

    /**
     * Compensation boolean.
     *
     * @param actionContext the action context
     * @return the boolean
     */
    boolean compensation(BusinessActionContext actionContext);
}
