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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AliasFor;

/**
 * declare the transaction only execute in single local RM
 * but the transaction need to ensure records to update(or select for update) is not in global transaction middle
 * stage
 *
 * use this annotation instead of GlobalTransaction in the situation mentioned above will help performance.
 *
 * @see io.seata.spring.annotation.GlobalTransactionScanner#wrapIfNecessary(Object, String, Object) // the scanner for TM, GlobalLock, and TCC mode
 * @see io.seata.spring.annotation.GlobalTransactionalInterceptor#handleGlobalLock(MethodInvocation, GlobalLock)  // the interceptor of GlobalLock
 * @see io.seata.spring.annotation.datasource.SeataAutoDataSourceProxyAdvice#invoke(MethodInvocation) // the interceptor of GlobalLockLogic and AT/XA mode
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.TYPE})
@Inherited
public @interface GlobalLock {
    /**
     * customized global lock retry interval(unit: ms)
     * you may use this to override global config of "client.rm.lock.retryInterval"
     * note: 0 or negative number will take no effect(which mean fall back to global config)
     * @return lock retry interval
     */
    int lockRetryInterval() default 0;

    /**
     * customized global lock retry interval(unit: ms)
     * you may use this to override global config of "client.rm.lock.retryInterval"
     * note: 0 or negative number will take no effect(which mean fall back to global config)
     * @return lock retry interval
     */
    @Deprecated
    @AliasFor("lockRetryInterval")
    int lockRetryInternal() default 0;

    /**
     * customized global lock retry times
     * you may use this to override global config of "client.rm.lock.retryTimes"
     * note: negative number will take no effect(which mean fall back to global config)
     * @return lock retry times
     */
    int lockRetryTimes() default -1;

}
