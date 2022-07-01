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
package io.seata.saga.api;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.BusinessActionContextUtil;
import io.seata.saga.interceptor.SagaActionInterceptor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;

/**
 * SAGA annotation.
 * Define a SAGA interface, which added on the saga commit method.
 * Must be used with `@LocalService`.
 *
 * @author ruishansun
 * @see io.seata.spring.annotation.LocalService // local transaction annotation, which added on the transaction interface. It can't be left out.
 * @see io.seata.spring.annotation.GlobalTransactionScanner#wrapIfNecessary(Object, String, Object) // the scanner for TM, GlobalLock, TCC mode, SAGA mode
 * @see SagaActionInterceptor // the interceptor of SAGA mode
 * @see BusinessActionContext
 * @see BusinessActionContextUtil
 * @see BusinessActionContextParameter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
public @interface SagaTransactional {

    /**
     * Saga bean name, must be unique
     *
     * @return the string
     */
    String name();

    /**
     * compensation method name
     *
     * @return the string
     */
    String compensationMethod() default "compensation";

    /**
     * delay branch report while sharing params to saga phase 2 to enhance performance
     *
     * @return isDelayReport
     */
    boolean isDelayReport() default false;

    /**
     * whether use common fence (idempotent,non_rollback,suspend)
     *
     * @return the boolean
     */
    boolean useCommonFence() default false;

    /**
     * compensation method's args
     *
     * @return the Class[]
     */
    Class<?>[] compensationArgsClasses() default {BusinessActionContext.class};
}
