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
package org.apache.seata.rm.tcc.utils;

import java.lang.reflect.Method;

import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Transactional;



public class MethodUtils {
    /**
     * Retrieve the Transactional annotation of a business method
     * @param interfaceMethod interface method object
     * @param targetTCCBean target tcc bean
     * @return the @Transactional annotation
     */
    public static Transactional getTransactionalAnnotationByMethod(Method interfaceMethod, Object targetTCCBean) {
        String methodName = interfaceMethod.getName();
        Class<?>[] parameterTypes = interfaceMethod.getParameterTypes();

        Class<?> clazz = targetTCCBean.getClass();
        Method implementationMethod;
        try {
            implementationMethod = clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new ShouldNeverHappenException(e);
        }
        return AnnotatedElementUtils.findMergedAnnotation(implementationMethod, Transactional.class);
    }
}
