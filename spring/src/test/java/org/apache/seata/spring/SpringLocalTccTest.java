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
package org.apache.seata.spring;

import java.io.IOException;
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.apache.seata.rm.tcc.interceptor.parser.TccActionInterceptorParser;
import org.apache.seata.spring.tcc.TccAnnoAtInterAction;
import org.apache.seata.spring.tcc.TccAnnoAtInterActionImpl;
import org.apache.seata.spring.tcc.TccAnnoAtInterImplAction;
import org.apache.seata.spring.tcc.TccAnnoAtInterImplActionImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;


public class SpringLocalTccTest {


    @BeforeAll
    public static void init() throws IOException {
        System.setProperty("config.type", "file");
        System.setProperty("config.file.name", "file.conf");
        System.setProperty("txServiceGroup", "default_tx_group");
        System.setProperty("service.vgroupMapping.default_tx_group", "default");
    }

    @Test
    void testParserInterfaceToProxyForSpringCGLIB() throws Exception {
        //local tcc anno at interface impl
        {
            TccActionInterceptorParser tccActionInterceptorParser = new TccActionInterceptorParser();
            TccAnnoAtInterImplActionImpl tccAction = new TccAnnoAtInterImplActionImpl();
            TccAnnoAtInterImplAction proxyTccAction = createSpringCGLIBProxy(tccAction, "doSomething", TccAnnoAtInterImplAction.class);
            ProxyInvocationHandler proxyInvocationHandler = tccActionInterceptorParser.parserInterfaceToProxy(proxyTccAction, proxyTccAction.getClass().getName());
            Assertions.assertNotNull(proxyInvocationHandler);
        }

        //local tcc anno at interface
        {
            TccActionInterceptorParser tccActionInterceptorParser = new TccActionInterceptorParser();
            TccAnnoAtInterActionImpl tccAction = new TccAnnoAtInterActionImpl();
            TccAnnoAtInterAction proxyTccAction = createSpringCGLIBProxy(tccAction, "doSomething", TccAnnoAtInterAction.class);
            ProxyInvocationHandler proxyInvocationHandler = tccActionInterceptorParser.parserInterfaceToProxy(proxyTccAction, proxyTccAction.getClass().getName());
            Assertions.assertNotNull(proxyInvocationHandler);
        }
    }

    private  <T> T createSpringCGLIBProxy(T target, String methodName, Class<T> interfaceClass) {
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        MethodBeforeAdvice advice = (method, args1, target1) -> System.out.println("test");
        NameMatchMethodPointcutAdvisor advisor = new NameMatchMethodPointcutAdvisor(advice);
        advisor.addMethodName(methodName);
        proxyFactory.addAdvisor(advisor);
        return interfaceClass.cast(proxyFactory.getProxy());
    }
}
