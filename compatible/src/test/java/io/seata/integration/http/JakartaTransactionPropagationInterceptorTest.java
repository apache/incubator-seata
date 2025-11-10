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
package io.seata.integration.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for JakartaTransactionPropagationInterceptor compatibility wrapper.
 */
public class JakartaTransactionPropagationInterceptorTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                JakartaTransactionPropagationInterceptor.class.isAnnotationPresent(Deprecated.class),
                "JakartaTransactionPropagationInterceptor should be marked as @Deprecated");
    }

    @Test
    public void testExtendsApacheSeataClass() {
        assertTrue(
                org.apache.seata.integration.http.JakartaTransactionPropagationInterceptor.class.isAssignableFrom(
                        JakartaTransactionPropagationInterceptor.class),
                "JakartaTransactionPropagationInterceptor should extend Apache Seata JakartaTransactionPropagationInterceptor");
    }

    @Test
    public void testConstructor() {
        JakartaTransactionPropagationInterceptor interceptor = new JakartaTransactionPropagationInterceptor();
        assertNotNull(interceptor);
    }

    @Test
    public void testInstanceOfApacheSeataClass() {
        JakartaTransactionPropagationInterceptor interceptor = new JakartaTransactionPropagationInterceptor();
        assertTrue(
                interceptor instanceof org.apache.seata.integration.http.JakartaTransactionPropagationInterceptor,
                "Instance should be of Apache Seata JakartaTransactionPropagationInterceptor type");
    }
}
