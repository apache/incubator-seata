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
package org.apache.seata.saga.rm.interceptor.parser;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.core.model.Resource;
import org.apache.seata.integration.tx.api.interceptor.ActionContextUtil;
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.apache.seata.integration.tx.api.interceptor.parser.IfNeedEnhanceBean;
import org.apache.seata.integration.tx.api.interceptor.parser.InterfaceParser;
import org.apache.seata.integration.tx.api.interceptor.parser.NeedEnhanceEnum;
import org.apache.seata.integration.tx.api.remoting.parser.DefaultRemotingParser;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.saga.rm.SagaAnnotationResource;
import org.apache.seata.saga.rm.api.CompensationBusinessAction;
import org.apache.seata.saga.rm.interceptor.SagaAnnotationActionInterceptorHandler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * saga-annotation proxyInvocationHandler parser
 * used to identify the saga annotation @CompensationBusinessAction and return the proxy handler.
 */
public class SagaAnnotationActionInterceptorParser implements InterfaceParser {

    @Override
    public ProxyInvocationHandler parserInterfaceToProxy(Object target, String objectName) {
        Map<Method, Class<?>> methodClassMap = ReflectionUtil.findMatchMethodClazzMap(target.getClass(), method -> method.isAnnotationPresent(getAnnotationClass()));
        Set<Method> methodsToProxy = methodClassMap.keySet();
        if (methodsToProxy.isEmpty()) {
            return null;
        }

        // register resource and enhance with interceptor
        registerResource(target, methodClassMap);

        return new SagaAnnotationActionInterceptorHandler(target, methodsToProxy.stream().map(Method::getName).collect(Collectors.toSet()));
    }

    private void registerResource(Object target, Map<Method, Class<?>> methodClassMap) {
        try {
            for (Map.Entry<Method, Class<?>> methodClassEntry : methodClassMap.entrySet()) {
                Method method = methodClassEntry.getKey();
                Annotation annotation = method.getAnnotation(getAnnotationClass());
                if (annotation != null) {
                    Resource resource = createResource(target, methodClassEntry.getValue(), annotation);
                    //registry resource
                    DefaultResourceManager.get().registerResource(resource);
                }
            }
        } catch (Throwable t) {
            throw new FrameworkException(t, "register SagaAnnotation resource error");
        }
    }


    @Override
    public IfNeedEnhanceBean parseIfNeedEnhancement(Class<?> beanClass) {
        IfNeedEnhanceBean ifNeedEnhanceBean = new IfNeedEnhanceBean();
        //current support remote service enhance
        if (DefaultRemotingParser.get().isService(beanClass)) {
            ifNeedEnhanceBean.setIfNeed(true);
            ifNeedEnhanceBean.setNeedEnhanceEnum(NeedEnhanceEnum.SERVICE_BEAN);
        }
        return ifNeedEnhanceBean;
    }

    protected Class<? extends Annotation> getAnnotationClass() {
        return CompensationBusinessAction.class;
    }

    protected Resource createResource(Object targetBean, Class<?> serviceClass, Annotation annotation) throws NoSuchMethodException {
        CompensationBusinessAction compensationBusinessAction = (CompensationBusinessAction) annotation;
        SagaAnnotationResource sagaAnnotationResource = new SagaAnnotationResource();
        sagaAnnotationResource.setActionName(compensationBusinessAction.name());
        sagaAnnotationResource.setTargetBean(targetBean);
        sagaAnnotationResource.setCompensationMethodName(compensationBusinessAction.compensationMethod());
        Method compensationMethod = serviceClass.getMethod(compensationBusinessAction.compensationMethod(), compensationBusinessAction.compensationArgsClasses());
        sagaAnnotationResource.setCompensationMethod(compensationMethod);
        sagaAnnotationResource.setCompensationArgsClasses(compensationBusinessAction.compensationArgsClasses());
        sagaAnnotationResource.setPhaseTwoCompensationKeys(ActionContextUtil.getTwoPhaseArgs(sagaAnnotationResource.getCompensationMethod(), compensationBusinessAction.compensationArgsClasses()));

        return sagaAnnotationResource;
    }
}
