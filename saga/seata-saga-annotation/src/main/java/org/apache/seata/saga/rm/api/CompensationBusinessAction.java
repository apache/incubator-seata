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
package org.apache.seata.saga.rm.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.seata.rm.tcc.api.BusinessActionContext;

/**
 * Saga annotation.
 * Define a saga interface, which added on the commit method.
 *
 * @author leezongjie
 * @date 2022/11/26
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
public @interface CompensationBusinessAction {

    /**
     * saga bean name, must be unique
     *
     * @return the string
     */
    String name();

    /**
     * rollback method name
     *
     * @return the string
     */
    String compensationMethod() default "compensation";

    /**
     * delay branch report while sharing params to phase 2 to enhance performance
     *
     * @return isDelayReport
     */
    boolean isDelayReport() default false;

    /**
     * whether use fence (idempotent,non_rollback,suspend)
     *
     * @return the boolean
     */
    boolean useFence() default false;

    /**
     * rollback method's args
     *
     * @return the Class[]
     */
    Class<?>[] compensationArgsClasses() default {BusinessActionContext.class};
}
