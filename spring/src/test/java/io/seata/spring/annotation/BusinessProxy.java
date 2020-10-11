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
package io.seata.spring.annotation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusinessProxy implements InvocationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessProxy.class);

    private Object proxy;

    public BusinessProxy(Object proxy) {
        this.proxy = proxy;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Before invoking proxy method.");
        }
        Object result = null;
        try {
            result = method.invoke(this.proxy, args);
        } catch (Exception e) {
            LOGGER.warn("Failed to invoke method {}.", method.getName());
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("After invoking proxy method.");
        }

        return result;
    }
}
