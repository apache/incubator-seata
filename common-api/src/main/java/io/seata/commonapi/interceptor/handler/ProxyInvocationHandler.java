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
package io.seata.commonapi.interceptor.handler;

import io.seata.commonapi.interceptor.InvocationWrapper;

import java.util.Set;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
public interface ProxyInvocationHandler {

    Class[] getInterfaceToProxy();

    Set<String> getMethodsToProxy();

    boolean interfaceProxyMode();

    Object invoke(InvocationWrapper invocation) throws Throwable;

}
