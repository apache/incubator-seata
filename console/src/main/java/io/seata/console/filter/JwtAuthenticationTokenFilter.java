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
package io.seata.console.filter;

import java.io.IOException;

import io.seata.console.constant.SecurityConstants;
import io.seata.console.utils.JwtTokenUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * jwt auth token filter
 *
 * @author jameslcj wfnuser
 */
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

	private final JwtTokenUtils tokenProvider;

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
			throws IOException, ServletException, ServletException {
		String jwt = resolveToken(request);

		if (jwt != null && !"".equals(jwt.trim()) && SecurityContextHolder.getContext().getAuthentication() == null) {
			if (this.tokenProvider.validateToken(jwt)) {
				/**
				 * get auth info
				 */
				Authentication authentication = this.tokenProvider.getAuthentication(jwt);
				/**
				 * save user info to securityContext
				 */
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		}

		chain.doFilter(request, response);
	}

	/**
	 * Get token from header
	 */
	private String resolveToken(HttpServletRequest request) {
		String bearerToken = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(SecurityConstants.TOKEN_PREFIX)) {
			return bearerToken.substring(SecurityConstants.TOKEN_PREFIX.length());
		}
		String jwt = request.getParameter(SecurityConstants.AUTHORIZATION_TOKEN);
		if (StringUtils.hasText(jwt)) {
			return jwt;
		}
		return null;
	}
}

