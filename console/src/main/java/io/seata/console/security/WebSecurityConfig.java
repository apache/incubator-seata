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
package io.seata.console.security;

import io.seata.console.filter.JwtAuthenticationTokenFilter;
import io.seata.console.utils.JwtTokenUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring security config
 *
 * @author jameslcj
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

	@Autowired
	private JwtAuthenticationEntryPoint unauthorizedHandler;

	@Autowired
	private JwtTokenUtils tokenProvider;

	@Autowired
	private CustomAuthenticationProvider authenticationProvider;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http
				.authorizeHttpRequests()
				// Ignore the static resources URL
				.requestMatchers(
						"/",
						"/*/*.css",
						"/*/*.js",
						"/*/*.map",
						"/*/*.svg",
						"/*/*.png",
						"/*/*.html",
						"/*/*.ico",
						"/*/*.jpeg",
						"/console-fe/public/**",
						"/api/v1/auth/login")
				.permitAll()
				.anyRequest()
				.authenticated()
				.and()
				// custom token authorize exception handler
				.exceptionHandling()
				.authenticationEntryPoint(unauthorizedHandler)
				.and()
				// since we use jwt, session is not necessary
				.sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				.and()
				// since we use jwt, csrf is not necessary
				.csrf()
				.disable()
				.authenticationProvider(authenticationProvider)
				.addFilterBefore(new JwtAuthenticationTokenFilter(tokenProvider),
						UsernamePasswordAuthenticationFilter.class);

		// disable cache
		http
				.headers()
				.cacheControl();

		return http.build();
	}

}
