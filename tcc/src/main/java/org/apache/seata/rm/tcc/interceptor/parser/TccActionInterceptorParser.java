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
package org.apache.seata.rm.tcc.interceptor.parser;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.model.Resource;
import org.apache.seata.integration.tx.api.interceptor.ActionContextUtil;
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.apache.seata.integration.tx.api.interceptor.parser.IfNeedEnhanceBean;
import org.apache.seata.integration.tx.api.interceptor.parser.InterfaceParser;
import org.apache.seata.integration.tx.api.interceptor.parser.NeedEnhanceEnum;
import org.apache.seata.integration.tx.api.remoting.parser.DefaultRemotingParser;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.rm.tcc.TCCResource;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.apache.seata.rm.tcc.interceptor.TccActionInterceptorHandler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TccActionInterceptorParser implements InterfaceParser {

    @Override
    public ProxyInvocationHandler parserInterfaceToProxy(Object target, String objectName) {
        Map<Method, Class<?>> methodClassMap = ReflectionUtil.findMatchMethodClazzMap(target.getClass(), method -> method.isAnnotationPresent(getAnnotationClass()));
        Set<Method> methodsToProxy = methodClassMap.keySet();
        if (methodsToProxy.isEmpty()) {
            return null;
        }

        // register resource and enhance with interceptor
        registerResource(target, methodClassMap);

        return new TccActionInterceptorHandler(target, methodsToProxy.stream().map(Method::getName).collect(Collectors.toSet()));
    }

    @Override
    public IfNeedEnhanceBean parseIfNeedEnhancement(Class<?> beanClass) {
        IfNeedEnhanceBean ifNeedEnhanceBean = new IfNeedEnhanceBean();
        if (DefaultRemotingParser.get().isService(beanClass)) {
            ifNeedEnhanceBean.setIfNeed(true);
            ifNeedEnhanceBean.setNeedEnhanceEnum(NeedEnhanceEnum.SERVICE_BEAN);
        }
        return ifNeedEnhanceBean;
    }

    protected void registerResource(Object target, Map<Method, Class<?>> methodClassMap) {
        try {
            for (Map.Entry<Method, Class<?>> methodClassEntry : methodClassMap.entrySet()) {
                Method method = methodClassEntry.getKey();
                Annotation annotation = method.getAnnotation(getAnnotationClass());
                if (annotation != null) {
                    Resource resource = createResource(target, methodClassEntry.getValue(), method, annotation);
                    //registry resource
                    DefaultResourceManager.get().registerResource(resource);
                }
            }
        } catch (Throwable t) {
            throw new FrameworkException(t, "register tcc resource error");
        }
    }


    protected Class<? extends Annotation> getAnnotationClass() {
        return TwoPhaseBusinessAction.class;
    }

    protected Resource createResource(Object target, Class<?> targetServiceClass, Method m, Annotation annotation) throws NoSuchMethodException {
        TwoPhaseBusinessAction twoPhaseBusinessAction = (TwoPhaseBusinessAction) annotation;
        TCCResource tccResource = new TCCResource();
        if (StringUtils.isBlank(twoPhaseBusinessAction.name())) {
            throw new FrameworkException("TCC bean name cannot be null or empty");
        }
        tccResource.setActionName(twoPhaseBusinessAction.name());
        tccResource.setTargetBean(target);
        tccResource.setPrepareMethod(m);
        tccResource.setCommitMethodName(twoPhaseBusinessAction.commitMethod());
        tccResource.setCommitMethod(targetServiceClass.getMethod(twoPhaseBusinessAction.commitMethod(),
                twoPhaseBusinessAction.commitArgsClasses()));
        tccResource.setRollbackMethodName(twoPhaseBusinessAction.rollbackMethod());
        tccResource.setRollbackMethod(targetServiceClass.getMethod(twoPhaseBusinessAction.rollbackMethod(),
                twoPhaseBusinessAction.rollbackArgsClasses()));
        // set argsClasses
        tccResource.setCommitArgsClasses(twoPhaseBusinessAction.commitArgsClasses());
        tccResource.setRollbackArgsClasses(twoPhaseBusinessAction.rollbackArgsClasses());
        // set phase two method's keys
        tccResource.setPhaseTwoCommitKeys(ActionContextUtil.getTwoPhaseArgs(tccResource.getCommitMethod(),
                twoPhaseBusinessAction.commitArgsClasses()));
        tccResource.setPhaseTwoRollbackKeys(ActionContextUtil.getTwoPhaseArgs(tccResource.getRollbackMethod(),
                twoPhaseBusinessAction.rollbackArgsClasses()));
        return tccResource;
    }
}
