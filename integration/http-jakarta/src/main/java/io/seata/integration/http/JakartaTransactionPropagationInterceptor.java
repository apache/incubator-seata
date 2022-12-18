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
package io.seata.integration.http;

import io.seata.core.context.RootContext;

/**
 * The Jakarta SpringMVC Interceptor.
 *
 * @author wangxb
 * @author wang.liang
 */
public class JakartaTransactionPropagationInterceptor extends TransactionPropagationInterceptor {

    //@Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) {
        String rpcXid = request.getHeader(RootContext.KEY_XID);
        return this.bindXid(rpcXid);
    }

    //@Override
    public void afterCompletion(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (RootContext.inGlobalTransaction()) {
            String rpcXid = request.getHeader(RootContext.KEY_XID);
            this.cleanXid(rpcXid);
        }
    }
}
