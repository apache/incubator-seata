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
package io.seata.spring.tcc;

import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.apache.seata.integration.tx.api.remoting.RemotingDesc;
import org.apache.seata.integration.tx.api.util.ProxyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@Deprecated
public class TccAnnotationProcessor extends org.apache.seata.spring.tcc.TccAnnotationProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(org.apache.seata.spring.tcc.TccAnnotationProcessor.class);

    @Override
    public void addTccAdvise(Object bean, String beanName, Field field, Class serviceClass) throws IllegalAccessException {
        Object fieldValue = field.get(bean);
        if (fieldValue == null) {
            return;
        }
        for (Method method : field.getType().getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) && (method.isAnnotationPresent(TwoPhaseBusinessAction.class))) {
                RemotingDesc remotingDesc = new RemotingDesc();
                remotingDesc.setServiceClass(serviceClass);

                Object proxyBean = ProxyUtil.createProxy(bean, beanName);
                field.setAccessible(true);
                field.set(bean, proxyBean);
                LOGGER.info("Bean[" + bean.getClass().getName() + "] with name [" + field.getName() + "] would use proxy");
            }
        }
    }
}
