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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.seata.config.ConfigurationCache;
import io.seata.config.ConfigurationChangeListener;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.integrationapi.interceptor.TxBeanParserUtils;
import io.seata.integrationapi.interceptor.handler.ProxyInvocationHandler;
import io.seata.integrationapi.interceptor.parser.DefaultResourceRegisterParser;
import io.seata.integrationapi.interceptor.parser.InterfaceParser;
import io.seata.integrationapi.remoting.RemotingDesc;
import io.seata.integrationapi.remoting.parser.DefaultRemotingParser;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import io.seata.rm.tcc.interceptor.TccActionInterceptorHandler;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
public class TccActionInterceptorParser implements InterfaceParser {

    @Override
    public ProxyInvocationHandler parserInterfaceToProxy(Object target) {
        boolean isTxRemotingBean = TxBeanParserUtils.isTxRemotingBean(target, target.toString());
        if (isTxRemotingBean) {
            RemotingDesc remotingDesc = DefaultRemotingParser.get().getRemotingBeanDesc(target);
            if (remotingDesc != null) {
                if (remotingDesc.isService()) {
                    DefaultResourceRegisterParser.get().registerResource(target);
                }
                if (remotingDesc.isReference()) {
                    //if it is a tcc remote reference
                    Set<String> methodsToProxy = tccProxyTargetMethod(remotingDesc);
                    if (remotingDesc != null && !methodsToProxy.isEmpty()) {
                        Class[] interfaceToProxy = target.getClass().getInterfaces();
                        ProxyInvocationHandler proxyInvocationHandler = new TccActionInterceptorHandler(interfaceToProxy, methodsToProxy);
                        ConfigurationCache.addConfigListener(ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION, (ConfigurationChangeListener) proxyInvocationHandler);
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
        Method[] methods = tccServiceClazz.getMethods();
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
