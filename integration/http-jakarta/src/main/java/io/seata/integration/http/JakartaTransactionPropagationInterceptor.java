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

import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Springmvc Intercepter.
 *
 * @author wangxb
 */
public class JakartaTransactionPropagationInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JakartaTransactionPropagationInterceptor.class);

    //@Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                             jakarta.servlet.http.HttpServletResponse response,
                             Object handler) {
        String xid = RootContext.getXID();
        String rpcXid = request.getHeader(RootContext.KEY_XID);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("xid in RootContext[{}] xid in HttpContext[{}]", xid, rpcXid);
        }
        if (StringUtils.isBlank(xid) && StringUtils.isNotBlank(rpcXid)) {
            RootContext.bind(rpcXid);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("bind[{}] to RootContext", rpcXid);
            }
        }

        return true;
    }

    //@Override
    public void afterCompletion(jakarta.servlet.http.HttpServletRequest request,
                                jakarta.servlet.http.HttpServletResponse response,
                                Object handler,
                                Exception ex) throws Exception {
        if (RootContext.inGlobalTransaction()) {
            XidResource.cleanXid(request.getHeader(RootContext.KEY_XID));
        }
    }

}
