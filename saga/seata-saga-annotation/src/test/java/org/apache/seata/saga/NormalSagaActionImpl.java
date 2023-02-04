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
package org.apache.seata.saga;

import org.apache.seata.rm.tcc.api.BusinessActionContext;

import java.util.List;


/**
 * @author leezongjie
 * @date 2022/12/9
 */
public class NormalSagaActionImpl implements NormalSagaAction {

    @Override
    public String prepare(BusinessActionContext actionContext, int a, List b, SagaParam sagaParam) {
        return "a";
    }

    @Override
    public boolean compensation(BusinessActionContext actionContext, SagaParam param) {
        return true;
    }
}
