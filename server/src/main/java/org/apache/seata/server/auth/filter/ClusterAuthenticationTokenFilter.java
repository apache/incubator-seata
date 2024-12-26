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
package org.apache.seata.server.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.result.Code;
import org.apache.seata.common.result.SingleResult;
import org.apache.seata.server.auth.config.ClusterSecurityConfig;
import org.apache.seata.server.auth.utils.ClusterJwtTokenUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * jwt auth token filter
 *
 */
public class ClusterAuthenticationTokenFilter extends OncePerRequestFilter {

    private ClusterJwtTokenUtils tokenProvider;

    /**
     * Instantiates a new Jwt authentication token filter.
     *
     * @param tokenProvider the token provider
     */
    public ClusterAuthenticationTokenFilter(ClusterJwtTokenUtils tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String accessToken = resolveAccessToken(request);
        String refreshToken = resolveRefreshToken(request);
        SingleResult result = new SingleResult(Code.CHECK_TOKEN_FAILED);
        ObjectMapper objectMapper = new ObjectMapper();
        if (accessToken != null) {
            result = this.tokenProvider.validateAccessToken(accessToken);
            if (result.getMessage().equals(Code.ACCESS_TOKEN_NEAR_EXPIRATION.getMsg())) {
                //access token is near expiration
                response.addHeader(ClusterSecurityConfig.ACCESS_TOKEN_NEAR_EXPIRATION, "true");
            }
        } else if (refreshToken != null) {
            result = this.tokenProvider.validateRefreshToken(refreshToken);
            if (result.getCode().equals(Code.SUCCESS.getCode())) {
                //create access token
                String newAccessToken = this.tokenProvider.createAccessToken((UsernamePasswordAuthenticationToken)result.getData());

                String authHeader = ClusterSecurityConfig.TOKEN_PREFIX + newAccessToken;
                //put token into http header
                response.addHeader(ClusterSecurityConfig.AUTHORIZATION_HEADER, authHeader);
            }
        }
        if (result.getCode().equals(Code.SUCCESS.getCode())) {
            /**
             * get auth info
             */
            Authentication authentication = (UsernamePasswordAuthenticationToken)result.getData();
            /**
             * save user info to securityContext
             */
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(objectMapper.writeValueAsString(result));
        }
    }

    /**
     * Get access token from header
     */
    private String resolveAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(ClusterSecurityConfig.AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(ClusterSecurityConfig.TOKEN_PREFIX)) {
            String accessToken = bearerToken.substring(ClusterSecurityConfig.TOKEN_PREFIX.length());
            return StringUtils.hasText(accessToken) ? accessToken : null;
        }
        String accessToken = request.getParameter(ClusterSecurityConfig.AUTHORIZATION_TOKEN);
        if (StringUtils.hasText(accessToken)) {
            return accessToken;
        }
        return null;
    }

    /**
     * Get refresh token from header
     */
    private String resolveRefreshToken(HttpServletRequest request) {
        String refreshToken = request.getHeader(ClusterSecurityConfig.REFRESH_TOKEN);
        return StringUtils.hasText(refreshToken) ? refreshToken : null;
    }
}

