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

import java.lang.annotation.Annotation;
import java.util.Set;
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.apache.seata.rm.tcc.interceptor.parser.TccActionInterceptorParser;
import org.apache.seata.saga.rm.api.CompensationBusinessAction;
import org.apache.seata.saga.rm.interceptor.SagaActionInterceptorHandler;

/**
 * saga-annotation proxyInvocationHandler parser, extended from TccActionInterceptorParser, used to identify the saga annotation {@link CompensationBusinessAction} and return the proxy handler.
 */
public class SagaActionInterceptorParser extends TccActionInterceptorParser {

    @Override
    protected ProxyInvocationHandler createProxyInvocationHandler(Object target, Set<String> methodsToProxy) {
        ProxyInvocationHandler proxyInvocationHandler = new SagaActionInterceptorHandler(target, methodsToProxy);
        return proxyInvocationHandler;
    }

    @Override
    protected Class<? extends Annotation> getAnnotationClass() {
        return CompensationBusinessAction.class;
    }
}
