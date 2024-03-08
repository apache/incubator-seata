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
package org.apache.seata.rm.tcc.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * the TCC parameters that need to be passed to the action context;
 * <p>
 * add this annotation on the parameters of the try method, and the parameters will be passed to the action context
 *
 * @see org.apache.seata.integration.tx.api.interceptor.ActionContextUtil
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface BusinessActionContextParameter {

    /**
     * parameter's name. Synonym for {@link #paramName()}.
     *
     * @return the name of the param or field
     * @see org.apache.seata.integration.tx.api.interceptor.ActionContextUtil#getParamNameFromAnnotation
     */
    String value() default "";

    /**
     * parameter's name. Synonym for {@link #value()}.
     *
     * @return the name of the param or field
     * @see org.apache.seata.integration.tx.api.interceptor.ActionContextUtil#getParamNameFromAnnotation
     */
    String paramName() default "";

    /**
     * if it is a sharding param ?
     *
     * @return the boolean
     * @deprecated This property is no longer in use.
     */
    @Deprecated
    boolean isShardingParam() default false;

    /**
     * Specify the index of the parameter in the List
     *
     * @return the index of the List
     * @see org.apache.seata.integration.tx.api.interceptor.ActionContextUtil#getByIndex
     */
    int index() default -1;

    /**
     * whether get the parameter from the property of the object
     * if {@code index >= 0}, the object get from the List and then do get the parameter from the property of the object
     *
     * @return the boolean
     * @see org.apache.seata.integration.tx.api.interceptor.ActionContextUtil#loadParamByAnnotationAndPutToContext
     * @see org.apache.seata.integration.tx.api.interceptor.ActionContextUtil#fetchContextFromObject
     */
    boolean isParamInProperty() default false;
}
