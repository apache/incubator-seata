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
package org.apache.seata.integration.tx.api.interceptor.handler;

import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.integration.tx.api.interceptor.InvocationWrapper;


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
