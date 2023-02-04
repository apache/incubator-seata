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
package org.apache.seata.saga.rm.interceptor.parser;

import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import org.apache.seata.saga.NormalSagaActionImpl;

/**
 * @author leezongjie
 * @date 2022/12/24
 */
class SagaActionInterceptorParserTest {

    @Test
    void parserInterfaceToProxy() {
        //given
        SagaActionInterceptorParser sagaActionInterceptorParser = new SagaActionInterceptorParser();

        NormalSagaActionImpl sagaAction = new NormalSagaActionImpl();

        //when
        ProxyInvocationHandler proxyInvocationHandler = sagaActionInterceptorParser.parserInterfaceToProxy(sagaAction,"sagaAction");

        //then
        Assertions.assertNotNull(proxyInvocationHandler);

    }
}