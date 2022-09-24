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
package io.seata.commonapi.autoproxy;

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
     * whether used common fence
     */
    private boolean useCommonFence;

    /**
     * transaction proxy method
     */
    private MethodInterceptor methodInterceptor;
    
    private ManualApiExecute manualApiExecute;

    public boolean isProxyTargetBean() {
        return isProxyTargetBean;
    }

    public void setProxyTargetBean(boolean proxyTargetBean) {
        isProxyTargetBean = proxyTargetBean;
    }

    public boolean isUseCommonFence() {
        return useCommonFence;
    }

    public void setUseCommonFence(boolean useCommonFence) {
        this.useCommonFence = useCommonFence;
    }

    public MethodInterceptor getMethodInterceptor() {
        return methodInterceptor;
    }

    public void setMethodInterceptor(MethodInterceptor methodInterceptor) {
        this.methodInterceptor = methodInterceptor;
    }

    public ManualApiExecute getManualApiExecute() {
        return manualApiExecute;
    }

    public void setManualApiExecute(ManualApiExecute manualApiExecute) {
        this.manualApiExecute = manualApiExecute;
    }
}