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
package org.apache.seata.rm.tcc;

import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextParameter;
import org.apache.seata.rm.tcc.api.LocalTCC;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;

import java.util.List;

/**
 * The interface Tcc action.
 *
 */
@LocalTCC
public interface NormalTccAction {

    /**
     * Prepare boolean.
     *
     * @param actionContext the action context
     * @param a             the a
     * @param b             the b
     * @param tccParam      the tcc param
     * @return the boolean
     */
    @TwoPhaseBusinessAction(name = "normalTccActionForTest", commitMethod = "commit", rollbackMethod = "rollback", commitArgsClasses = {BusinessActionContext.class, TccParam.class}, rollbackArgsClasses = {BusinessActionContext.class, TccParam.class})
    String prepare(BusinessActionContext actionContext,
                    @BusinessActionContextParameter("a") int a,
                    @BusinessActionContextParameter(paramName = "b", index = 0) List b,
                    @BusinessActionContextParameter(isParamInProperty = true) TccParam tccParam);

    /**
     * Commit boolean.
     *
     * @param actionContext the action context
     * @return the boolean
     */
    boolean commit(BusinessActionContext actionContext,
                   @BusinessActionContextParameter("tccParam") TccParam param);

    /**
     * Rollback boolean.
     *
     * @param actionContext the action context
     * @return the boolean
     */
    boolean rollback(BusinessActionContext actionContext, @BusinessActionContextParameter("tccParam") TccParam param);


    @TwoPhaseBusinessAction(name = "tccActionForTestWithException", commitMethod = "commit", rollbackMethod = "rollback", commitArgsClasses = {BusinessActionContext.class, TccParam.class}, rollbackArgsClasses = {BusinessActionContext.class, TccParam.class})
    String prepareWithException(BusinessActionContext actionContext,
                   @BusinessActionContextParameter("a") int a,
                   @BusinessActionContextParameter(paramName = "b", index = 0) List b,
                   @BusinessActionContextParameter(isParamInProperty = true) TccParam tccParam);

}
