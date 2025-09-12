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

import org.apache.seata.console.config.WebSecurityConfig;
import org.apache.seata.console.utils.JwtTokenUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class MCPJwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private final JwtTokenUtils tokenProvider;

    /**
     * Instantiates a new Jwt authentication token filter.
     *
     * @param tokenProvider the token provider
     */
    public MCPJwtAuthenticationTokenFilter(JwtTokenUtils tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain chain)
            throws IOException, ServletException {
        String jwt = resolveToken(request);

        if (jwt != null && !jwt.trim().isEmpty()) {
            if (!this.tokenProvider.validateToken(jwt)) {
                if (!response.isCommitted()) response.sendError(HttpStatus.UNAUTHORIZED.value(), "UnAuthorized");
            }
        } else {
            if (!response.isCommitted())
                response.sendError(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Do not carry JWT tokens, Please go to console first to get token");
        }

        chain.doFilter(request, response);
    }

    /**
     * Get token from header
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(WebSecurityConfig.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(WebSecurityConfig.TOKEN_PREFIX)) {
            return bearerToken.substring(WebSecurityConfig.TOKEN_PREFIX.length());
        }
        String jwt = request.getParameter(WebSecurityConfig.AUTHORIZATION_TOKEN);
        if (StringUtils.hasText(jwt)) {
            return jwt;
        }
        return null;
    }
}
