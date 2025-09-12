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
package org.apache.seata.integration.tx.api.interceptor.parser;

import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.integration.tx.api.interceptor.handler.CombineTransactionalInterceptorHandler;
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.apache.seata.spring.annotation.CombineTransactional;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class CombineTransactionalInterceptorParser implements InterfaceParser {

    protected final Set<String> methodsToProxy = new HashSet<>();

    /**
     * @param target
     * @return
     * @throws Exception
     * @see CombineTransactional // Combine annotation
     */
    @Override
    public ProxyInvocationHandler parserInterfaceToProxy(Object target, String objectName) throws Exception {
        Class<?> serviceInterface = DefaultTargetClassParser.get().findTargetClass(target);
        Class<?>[] interfacesIfJdk = DefaultTargetClassParser.get().findInterfaces(target);

        if (existsAnnotation(serviceInterface) || existsAnnotation(interfacesIfJdk)) {
            return createProxyInvocationHandler();
        }

        return null;
    }

    protected ProxyInvocationHandler createProxyInvocationHandler() {
        return new CombineTransactionalInterceptorHandler(methodsToProxy);
    }

    @Override
    public IfNeedEnhanceBean parseIfNeedEnhancement(Class<?> beanClass) {
        Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(beanClass);
        Class<?>[] interfaceClasseArray = interfaceClasses.toArray(new Class<?>[0]);

        IfNeedEnhanceBean ifNeedEnhanceBean = new IfNeedEnhanceBean();
        if (existsAnnotation(beanClass) || existsAnnotation(interfaceClasseArray)) {
            ifNeedEnhanceBean.setIfNeed(true);
            ifNeedEnhanceBean.setNeedEnhanceEnum(NeedEnhanceEnum.SERVICE_BEAN);
        }
        return ifNeedEnhanceBean;
    }

    protected boolean existsAnnotation(Class<?>... classes) {
        boolean result = false;
        if (CollectionUtils.isNotEmpty(classes)) {
            for (Class<?> clazz : classes) {
                if (clazz == null) {
                    continue;
                }
                CombineTransactional trxAnno = clazz.getAnnotation(CombineTransactional.class);
                if (trxAnno != null) {
                    return true;
                }
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    trxAnno = method.getAnnotation(CombineTransactional.class);
                    if (trxAnno != null) {
                        methodsToProxy.add(method.getName());
                        result = true;
                    }
                }
            }
        }
        return result;
    }
}
