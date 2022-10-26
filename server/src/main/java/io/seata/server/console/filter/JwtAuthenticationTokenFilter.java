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
package io.seata.server.console.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @description: filter in server
 *
 * @author: jameslcj wfnuser sher
 */

@WebFilter(filterName = "serverRequestFilter", urlPatterns = "/server/*")
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private JwtTokenUtils tokenProvider;

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationTokenFilter.class);

    /**
     * Instantiates a new Jwt authentication token filter.
     *
     * @param tokenProvider the token provider
     */
    public JwtAuthenticationTokenFilter(JwtTokenUtils tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String jwt = resolveToken(request);

        if (jwt != null && !"".equals(jwt.trim())) {
            if (this.tokenProvider.validateToken(jwt)) {

                chain.doFilter(request, response);
//
            } else {
                LOGGER.error("validateToken error");
                throw new RuntimeException("validateToken error");
            }
        } else {
            LOGGER.error("jwt error");
            throw new RuntimeException("jwt error");
        }

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

