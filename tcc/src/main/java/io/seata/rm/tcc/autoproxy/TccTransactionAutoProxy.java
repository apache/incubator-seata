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
package io.seata.rm.tcc.autoproxy;

import io.seata.rm.tcc.interceptor.TCCBeanParserUtils;
import io.seata.rm.tcc.interceptor.TccActionInterceptor;
import io.seata.spring.autoproxy.TransactionAutoProxy;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.context.ApplicationContext;

/**
 * the tcc implements of TransactionAutoProxy
 *
 * @author ruishansun
 */
public class TccTransactionAutoProxy implements TransactionAutoProxy {

    @Override
    public MethodInterceptor isTransactionAutoProxy(Object bean, String beanName, ApplicationContext applicationContext) {
        if (TCCBeanParserUtils.isTccAutoProxy(bean, beanName, applicationContext)) {
            // init tcc fence clean task if enable useTccFence
            TCCBeanParserUtils.initTccFenceCleanTask(TCCBeanParserUtils.getRemotingDesc(beanName), applicationContext);
            //TCC interceptor, proxy bean of sofa:reference/dubbo:reference, and LocalTCC
            return new TccActionInterceptor(TCCBeanParserUtils.getRemotingDesc(beanName));
        }
        return null;
    }
}
