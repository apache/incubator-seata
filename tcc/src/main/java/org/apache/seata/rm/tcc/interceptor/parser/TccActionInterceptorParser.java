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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.apache.seata.integration.tx.api.interceptor.parser.DefaultResourceRegisterParser;
import org.apache.seata.integration.tx.api.interceptor.parser.IfNeedEnhanceBean;
import org.apache.seata.integration.tx.api.interceptor.parser.InterfaceParser;
import org.apache.seata.integration.tx.api.interceptor.parser.NeedEnhanceEnum;
import org.apache.seata.integration.tx.api.remoting.parser.DefaultRemotingParser;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.apache.seata.rm.tcc.interceptor.TccActionInterceptorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TccActionInterceptorParser implements InterfaceParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(TccActionInterceptorParser.class);

    @Override
    public ProxyInvocationHandler parserInterfaceToProxy(Object target, String objectName) {
        // eliminate the bean without two phase annotation.
        Set<String> methodsToProxy = this.tccProxyTargetMethod(target);
        if (methodsToProxy.isEmpty()) {
            return null;
        }
        // register resource and enhance with interceptor
        DefaultResourceRegisterParser.get().registerResource(target, objectName);
        return new TccActionInterceptorHandler(target, methodsToProxy);
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

    /**
     * is TCC proxy-bean/target-bean: LocalTCC , the proxy bean of sofa:reference/dubbo:reference
     *
     * @param target the remoting desc
     * @return boolean boolean
     */

    private Set<String> tccProxyTargetMethod(Object target) {
        Set<String> methodsToProxy = new HashSet<>();
        //check if it is TCC bean
        Class<?> tccServiceClazz = target.getClass();
        Set<Method> methods = new HashSet<>(Arrays.asList(tccServiceClazz.getMethods()));
        Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(tccServiceClazz);
        if (interfaceClasses != null) {
            for (Class<?> interClass : interfaceClasses) {
                methods.addAll(Arrays.asList(interClass.getMethods()));
            }
        }

        TwoPhaseBusinessAction twoPhaseBusinessAction;
        for (Method method : methods) {
            twoPhaseBusinessAction = method.getAnnotation(TwoPhaseBusinessAction.class);
            if (twoPhaseBusinessAction != null) {
                methodsToProxy.add(method.getName());
            }
        }

        if (methodsToProxy.isEmpty()) {
            return Collections.emptySet();
        }
        // sofa:reference /  dubbo:reference, AOP
        return methodsToProxy;
    }
}
