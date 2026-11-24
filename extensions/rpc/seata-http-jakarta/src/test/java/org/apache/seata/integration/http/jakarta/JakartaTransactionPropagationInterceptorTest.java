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
package org.apache.seata.integration.http.jakarta;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.seata.core.context.RootContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JakartaTransactionPropagationInterceptorTest {

    private static final String DEFAULT_XID = "127.0.0.1:8091:12345678";

    @AfterEach
    void cleanup() {
        RootContext.unbind();
    }

    @Test
    void testPreHandle_bindsXid() throws Exception {
        JakartaTransactionPropagationInterceptor interceptor = new JakartaTransactionPropagationInterceptor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader(RootContext.KEY_XID)).thenReturn(DEFAULT_XID);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(RootContext.getXID()).isEqualTo(DEFAULT_XID);
    }

    @Test
    void testPreHandle_noXid() throws Exception {
        JakartaTransactionPropagationInterceptor interceptor = new JakartaTransactionPropagationInterceptor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader(RootContext.KEY_XID)).thenReturn(null);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(RootContext.getXID()).isNull();
    }

    @Test
    void testPreHandle_doesNotOverrideExistingXid() throws Exception {
        RootContext.bind("existing-xid");
        JakartaTransactionPropagationInterceptor interceptor = new JakartaTransactionPropagationInterceptor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader(RootContext.KEY_XID)).thenReturn(DEFAULT_XID);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(RootContext.getXID()).isEqualTo("existing-xid");
    }

    @Test
    void testAfterCompletion_unbindsXid() throws Exception {
        RootContext.bind(DEFAULT_XID);
        JakartaTransactionPropagationInterceptor interceptor = new JakartaTransactionPropagationInterceptor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader(RootContext.KEY_XID)).thenReturn(DEFAULT_XID);

        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(RootContext.getXID()).isNull();
    }

    @Test
    void testAfterCompletion_noXidInContext() throws Exception {
        JakartaTransactionPropagationInterceptor interceptor = new JakartaTransactionPropagationInterceptor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader(RootContext.KEY_XID)).thenReturn(DEFAULT_XID);

        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(RootContext.getXID()).isNull();
    }

    @Test
    void testFullLifecycle() throws Exception {
        JakartaTransactionPropagationInterceptor interceptor = new JakartaTransactionPropagationInterceptor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader(RootContext.KEY_XID)).thenReturn(DEFAULT_XID);

        interceptor.preHandle(request, response, new Object());
        assertThat(RootContext.getXID()).isEqualTo(DEFAULT_XID);

        interceptor.afterCompletion(request, response, new Object(), null);
        assertThat(RootContext.getXID()).isNull();
    }
}
