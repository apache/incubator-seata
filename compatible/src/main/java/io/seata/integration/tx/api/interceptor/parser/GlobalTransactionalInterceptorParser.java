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
package io.seata.integration.tx.api.interceptor.parser;

import io.seata.spring.annotation.GlobalLock;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.seata.common.util.CollectionUtils;

import java.lang.reflect.Method;

public class GlobalTransactionalInterceptorParser extends org.apache.seata.integration.tx.api.interceptor.parser.GlobalTransactionalInterceptorParser{

    @Override
    protected boolean existsAnnotation(Class<?>... classes) {
        boolean result = false;
        if (CollectionUtils.isNotEmpty(classes)) {
            for (Class<?> clazz : classes) {
                if (clazz == null) {
                    continue;
                }
                GlobalTransactional trxAnnoOld = clazz.getAnnotation(GlobalTransactional.class);
                org.apache.seata.spring.annotation.GlobalTransactional trxAnnoNew = clazz.getAnnotation(org.apache.seata.spring.annotation.GlobalTransactional.class);

                if (trxAnnoOld != null || trxAnnoNew != null) {
                    return true;
                }
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    trxAnnoOld = method.getAnnotation(GlobalTransactional.class);
                    trxAnnoNew = method.getAnnotation(org.apache.seata.spring.annotation.GlobalTransactional.class);
                    if (trxAnnoOld != null || trxAnnoNew != null) {
                        methodsToProxy.add(method.getName());
                        result = true;
                    }

                    GlobalLock lockAnnoOld = method.getAnnotation(GlobalLock.class);
                    org.apache.seata.spring.annotation.GlobalLock lockAnnoNew = method.getAnnotation(org.apache.seata.spring.annotation.GlobalLock.class);

                    if (lockAnnoOld != null || lockAnnoNew != null) {
                        methodsToProxy.add(method.getName());
                        result = true;
                    }
                }
            }
        }
        return result;
    }
}
