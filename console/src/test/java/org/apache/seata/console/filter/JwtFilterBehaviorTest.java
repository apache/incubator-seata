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
package org.apache.seata.console.filter;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import org.apache.seata.console.utils.JwtTokenUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtFilterBehaviorTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tokenResolutionAndValidationDoNotBlockTheFilterChain() throws Exception {
        for (String header : new String[] {null, "Basic ignored", "Bearer ", "Bearer invalid", "Bearer valid"}) {
            JwtTokenUtils tokens = mock(JwtTokenUtils.class);
            when(tokens.validateToken("valid")).thenReturn(true);
            Authentication auth = new UsernamePasswordAuthenticationToken("user", "valid");
            when(tokens.getAuthentication("valid")).thenReturn(auth);
            MockHttpServletRequest request = new MockHttpServletRequest();
            if (header != null) {
                request.addHeader("Authorization", header);
            }
            FilterChain chain = mock(FilterChain.class);
            MockHttpServletResponse response = new MockHttpServletResponse();
            new JwtAuthenticationTokenFilter(tokens).doFilter(request, response, chain);
            verify(chain).doFilter(request, response);
            assertEquals(
                    "Bearer valid".equals(header) ? auth : null,
                    SecurityContextHolder.getContext().getAuthentication());
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void queryTokenWorksDuringAsyncDispatchAndExistingAuthenticationIsPreserved() throws Exception {
        JwtTokenUtils tokens = mock(JwtTokenUtils.class);
        when(tokens.validateToken("query-token")).thenReturn(true);
        Authentication auth = new UsernamePasswordAuthenticationToken("user", "query-token");
        when(tokens.getAuthentication("query-token")).thenReturn(auth);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setDispatcherType(DispatcherType.ASYNC);
        request.setParameter("access_token", "query-token");
        JwtAuthenticationTokenFilter filter = new JwtAuthenticationTokenFilter(tokens);
        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));
        assertSame(auth, SecurityContextHolder.getContext().getAuthentication());
        clearInvocations(tokens);
        filter.doFilter(
                new MockHttpServletRequest() {
                    @Override
                    public String getHeader(String name) {
                        return "Bearer other";
                    }
                },
                new MockHttpServletResponse(),
                mock(FilterChain.class));
        verifyNoInteractions(tokens);
        assertSame(auth, SecurityContextHolder.getContext().getAuthentication());
    }
}
