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
package org.apache.seata.rm.tcc.resource.parser;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.integration.tx.api.interceptor.ActionContextUtil;
import org.apache.seata.integration.tx.api.interceptor.parser.RegisterResourceParser;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.rm.tcc.TCCResource;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextParameter;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;

public class TccRegisterResourceParser implements RegisterResourceParser {

    @Override
    public void registerResource(Object target, String beanName) {
        try {
            //service bean, registry resource
            Class<?> serviceClass = target.getClass();
            this.executeRegisterResource(target, new HashSet<>(Arrays.asList(serviceClass.getMethods())), target.getClass());
            Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(serviceClass);
            for (Class<?> interClass : interfaceClasses) {
                this.executeRegisterResource(target, new HashSet<>(Arrays.asList(interClass.getMethods())), interClass);
            }
        } catch (Throwable t) {
            throw new FrameworkException(t, "parser remoting service error");
        }
    }

    private void executeRegisterResource(Object target, Set<Method> methods, Class<?> targetServiceClass) throws NoSuchMethodException {
        for (Method m : methods) {
            TwoPhaseBusinessAction twoPhaseBusinessAction = m.getAnnotation(TwoPhaseBusinessAction.class);
            if (twoPhaseBusinessAction != null) {
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
                tccResource.setPhaseTwoCommitKeys(this.getTwoPhaseArgs(tccResource.getCommitMethod(),
                        twoPhaseBusinessAction.commitArgsClasses()));
                tccResource.setPhaseTwoRollbackKeys(this.getTwoPhaseArgs(tccResource.getRollbackMethod(),
                        twoPhaseBusinessAction.rollbackArgsClasses()));
                //registry tcc resource
                DefaultResourceManager.get().registerResource(tccResource);
            }
        }
    }

    protected String[] getTwoPhaseArgs(Method method, Class<?>[] argsClasses) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        String[] keys = new String[parameterAnnotations.length];
        /*
         * get parameter's key
         * if method's parameter list is like
         * (BusinessActionContext, @BusinessActionContextParameter("a") A a, @BusinessActionContextParameter("b") B b)
         * the keys will be [null, a, b]
         */
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (int j = 0; j < parameterAnnotations[i].length; j++) {
                if (parameterAnnotations[i][j] instanceof BusinessActionContextParameter) {
                    BusinessActionContextParameter param = (BusinessActionContextParameter) parameterAnnotations[i][j];
                    String key = ActionContextUtil.getParamNameFromAnnotation(param);
                    keys[i] = key;
                    break;
                }
            }
            if (keys[i] == null && !(argsClasses[i].equals(BusinessActionContext.class))) {
                throw new IllegalArgumentException("non-BusinessActionContext parameter should use annotation " +
                        "BusinessActionContextParameter");
            }
        }
        return keys;
    }

}
