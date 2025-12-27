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
package org.apache.seata.common.loader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The type Enhanced service not found exception test.
 */
public class EnhancedServiceNotFoundExceptionTest {

    @Test
    public void testConstructorWithErrorCode() {
        String errorCode = "SERVICE_NOT_FOUND";
        EnhancedServiceNotFoundException exception = new EnhancedServiceNotFoundException(errorCode);

        assertNotNull(exception);
        assertEquals(errorCode, exception.getMessage());
    }

    @Test
    public void testConstructorWithErrorCodeAndCause() {
        String errorCode = "SERVICE_NOT_FOUND";
        Throwable cause = new RuntimeException("Root cause");
        EnhancedServiceNotFoundException exception = new EnhancedServiceNotFoundException(errorCode, cause);

        assertNotNull(exception);
        assertEquals(errorCode, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithErrorCodeAndErrorDesc() {
        String errorCode = "SERVICE_NOT_FOUND";
        String errorDesc = "Service not found in classpath";
        EnhancedServiceNotFoundException exception = new EnhancedServiceNotFoundException(errorCode, errorDesc);

        assertNotNull(exception);
        assertEquals(errorCode + ":" + errorDesc, exception.getMessage());
    }

    @Test
    public void testConstructorWithErrorCodeErrorDescAndCause() {
        String errorCode = "SERVICE_NOT_FOUND";
        String errorDesc = "Service not found in classpath";
        Throwable cause = new RuntimeException("Root cause");
        EnhancedServiceNotFoundException exception = new EnhancedServiceNotFoundException(errorCode, errorDesc, cause);

        assertNotNull(exception);
        assertEquals(errorCode + ":" + errorDesc, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithCause() {
        Throwable cause = new RuntimeException("Root cause");
        EnhancedServiceNotFoundException exception = new EnhancedServiceNotFoundException(cause);

        assertNotNull(exception);
        assertSame(cause, exception.getCause());
        assertEquals(cause.toString(), exception.getMessage());
    }

    @Test
    public void testFillInStackTrace() {
        EnhancedServiceNotFoundException exception = new EnhancedServiceNotFoundException("TEST_ERROR");
        Throwable result = exception.fillInStackTrace();

        // Should return the same instance
        assertSame(exception, result);
    }

    @Test
    public void testSerialVersionUID() throws NoSuchFieldException, IllegalAccessException {
        // Verify that serialVersionUID is correctly defined
        java.lang.reflect.Field field = EnhancedServiceNotFoundException.class.getDeclaredField("serialVersionUID");
        field.setAccessible(true);
        Long serialVersionUID = (Long) field.get(null);
        assertEquals(7748438218914409019L, serialVersionUID.longValue());
    }
}
