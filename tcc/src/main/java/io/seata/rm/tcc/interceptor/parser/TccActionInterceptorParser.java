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
package io.seata.rm.tcc.interceptor.parser;

import io.seata.common.util.ReflectionUtil;
import io.seata.integration.tx.api.interceptor.TxBeanParserUtils;
import io.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import io.seata.integration.tx.api.interceptor.parser.DefaultResourceRegisterParser;
import io.seata.integration.tx.api.interceptor.parser.InterfaceParser;
import io.seata.integration.tx.api.remoting.RemotingDesc;
import io.seata.integration.tx.api.remoting.parser.DefaultRemotingParser;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import io.seata.rm.tcc.interceptor.TccActionInterceptorHandler;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author leezongjie
 */
public class TccActionInterceptorParser implements InterfaceParser {

    @Override
    public ProxyInvocationHandler parserInterfaceToProxy(Object target, String beanName) {
        boolean isTxRemotingBean = TxBeanParserUtils.isTxRemotingBean(target, beanName);
        if (isTxRemotingBean) {
            RemotingDesc remotingDesc = DefaultRemotingParser.get().getRemotingBeanDesc(target);
            if (remotingDesc != null) {
                if (remotingDesc.isService()) {
                    DefaultResourceRegisterParser.get().registerResource(target, beanName);
                }
                if (remotingDesc.isReference()) {
                    //if it is a tcc remote reference
                    Set<String> methodsToProxy = tccProxyTargetMethod(remotingDesc);
                    if (remotingDesc != null && !methodsToProxy.isEmpty()) {
                        ProxyInvocationHandler proxyInvocationHandler = new TccActionInterceptorHandler(remotingDesc, methodsToProxy);
                        return proxyInvocationHandler;
                    }
                }
            }
        }
        return null;
    }

    /**
     * is TCC proxy-bean/target-bean: LocalTCC , the proxy bean of sofa:reference/dubbo:reference
     *
     * @param remotingDesc the remoting desc
     * @return boolean boolean
     */
    private Set<String> tccProxyTargetMethod(RemotingDesc remotingDesc) {
        if (!remotingDesc.isReference() || remotingDesc == null) {
            return Collections.emptySet();
        }
        Set<String> methodsToProxy = new HashSet<>();
        //check if it is TCC bean
        Class<?> tccServiceClazz = remotingDesc.getServiceClass();
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
