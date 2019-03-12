/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.rm.tcc;

import com.alibaba.fescar.rm.tcc.api.BusinessActionContext;
import com.alibaba.fescar.rm.tcc.api.BusinessActionContextParameter;
import com.alibaba.fescar.rm.tcc.api.LocalTCC;
import com.alibaba.fescar.rm.tcc.api.TwoPhaseBusinessAction;

import java.util.List;

/**
 * @author zhangsen
 */
@LocalTCC
public interface TccAction {

    @TwoPhaseBusinessAction(name = "tccActionForTest" , commitMethod = "commit", rollbackMethod = "rollback")
    public boolean prepare(BusinessActionContext actionContext,
                           @BusinessActionContextParameter(paramName = "a") int a,
                           @BusinessActionContextParameter(paramName = "b", index = 0) List b,
                           @BusinessActionContextParameter(isParamInProperty = true) TccParam tccParam);


    public boolean commit(BusinessActionContext actionContext);


    public boolean rollback(BusinessActionContext actionContext);
}
