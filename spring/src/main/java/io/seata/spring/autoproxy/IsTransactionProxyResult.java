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
package io.seata.spring.autoproxy;

import org.aopalliance.intercept.MethodInterceptor;

/**
 * whether is the transaction proxy result
 *
 * @author ruishansun
 */
public class IsTransactionProxyResult {

    /**
     * whether proxied by transaction bean
     */
    private boolean isProxyTargetBean;

    /**
     * whether used fence
     */
    private boolean useFence;

    /**
     * transaction proxy method
     */
    private MethodInterceptor methodInterceptor;

    public boolean isProxyTargetBean() {
        return isProxyTargetBean;
    }

    public void setProxyTargetBean(boolean proxyTargetBean) {
        isProxyTargetBean = proxyTargetBean;
    }

    public boolean isUseFence() {
        return useFence;
    }

    public void setUseFence(boolean useFence) {
        this.useFence = useFence;
    }

    public MethodInterceptor getMethodInterceptor() {
        return methodInterceptor;
    }

    public void setMethodInterceptor(MethodInterceptor methodInterceptor) {
        this.methodInterceptor = methodInterceptor;
    }
}
