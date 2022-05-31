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
import org.springframework.context.ApplicationContext;

/**
 * The interface Transaction Auto Proxy.
 * if result is not null, then proxied by tcc/saga with SPI.
 *
 * @author ruishansun
 */
public interface TransactionAutoProxy {

    /**
     * if it is transaction auto proxy? (tcc or saga)
     *
     * @param bean               the bean
     * @param beanName           the beanName
     * @param applicationContext the applicationContext
     * @return the MethodInterceptor
     */
    MethodInterceptor isTransactionAutoProxy(Object bean, String beanName, ApplicationContext applicationContext);
}
