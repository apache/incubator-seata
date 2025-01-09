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
package org.apache.seata.common.exception;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ExceptionUtilTest {
    private Exception exception;

    @Test
    public void unwrap() {
        InvocationTargetException targetException = new InvocationTargetException(new RuntimeException("invocation"));
        Assertions.assertInstanceOf(RuntimeException.class, ExceptionUtil.unwrap(targetException));

        UndeclaredThrowableException exception = new UndeclaredThrowableException(new RuntimeException("undeclared"));
        Assertions.assertInstanceOf(RuntimeException.class, ExceptionUtil.unwrap(exception));

        RuntimeException runtimeException = new RuntimeException("runtime");
        Assertions.assertInstanceOf(RuntimeException.class, ExceptionUtil.unwrap(runtimeException));
    }

    @Test
    public void unwrapInvocationTargetException() {
        InvocationTargetException ite = new InvocationTargetException(exception, "test");
        Throwable result = ExceptionUtil.unwrap(ite);
        Assertions.assertSame(exception, result, "Expected the unwrapped exception to be the cause of InvocationTargetException.");
    }

    @Test
    public void unwrapUndeclaredThrowableException() {
        UndeclaredThrowableException ute = new UndeclaredThrowableException(exception, "test");
        Throwable result = ExceptionUtil.unwrap(ute);
        Assertions.assertSame(exception, result, "Expected the unwrapped exception to be the cause of UndeclaredThrowableException.");
    }

    @Test
    public void unwrapNestedInvocationTargetException() {
        Exception rootCause = new Exception();
        InvocationTargetException ite = new InvocationTargetException(new UndeclaredThrowableException(rootCause, "test"), "test");
        Throwable result = ExceptionUtil.unwrap(ite);
        Assertions.assertSame(rootCause, result, "Expected the unwrapped exception to be the root cause.");
    }

    @Test
    public void unwrapNotWrappedException() {
        Throwable result = ExceptionUtil.unwrap(exception);
        Assertions.assertSame(exception, result, "Expected the unwrapped exception to be the same as the input when no wrapping is present.");
    }
}