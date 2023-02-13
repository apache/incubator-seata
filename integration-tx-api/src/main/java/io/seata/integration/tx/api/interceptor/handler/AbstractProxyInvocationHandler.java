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
package io.seata.integration.tx.api.interceptor.handler;

import io.seata.common.util.CollectionUtils;
import io.seata.integration.tx.api.interceptor.InvocationWrapper;

/**
 * @author leezongjie
 */
public abstract class AbstractProxyInvocationHandler implements ProxyInvocationHandler {

    protected abstract Object doInvoke(InvocationWrapper invocation) throws Throwable;

    protected int order = Integer.MAX_VALUE;

    @Override
    public Object invoke(InvocationWrapper invocation) throws Throwable {
        if (CollectionUtils.isNotEmpty(getMethodsToProxy()) && !getMethodsToProxy().contains(invocation.getMethod().getName())) {
            return invocation.proceed();
        }
        return doInvoke(invocation);
    }

    @Override
    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

}
