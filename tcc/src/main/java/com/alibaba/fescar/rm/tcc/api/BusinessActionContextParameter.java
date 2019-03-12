/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.rm.tcc.api;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * the TCC parameters that need to be passed to  the BusinessActivityContext；
 *
 * add this annotation on the parameters of the try method, and the parameters will be passed to  the
 * BusinessActivityContext；
 *
 * @author zhangsen
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.FIELD })
public @interface BusinessActionContextParameter {

    /**
     * parameter's name
     *
     * @return the string
     */
    String paramName() default "";

    /**
     * if it is a sharding param ?
     *
     * @return boolean
     */
    boolean isShardingParam() default false;

    /**
     * Specify the index of the parameter in the List
     *
     * @return int
     */
    int index() default -1;

    /**
     * if get the parameter from the property of the object ?
     *
     * @return boolean
     */
    boolean isParamInProperty() default false;
}