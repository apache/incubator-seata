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
package org.apache.seata.console.config;

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.console.filter.ConsoleAuthenticationTokenFilter;
import org.apache.seata.console.security.CustomUserDetailsServiceImpl;
import org.apache.seata.console.security.JwtAuthenticationEntryPoint;
import org.apache.seata.console.utils.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Spring security config
 *
 */
@Configuration(proxyBeanMethods = false)
@Order(2)
public class ConsoleSecurityConfig extends WebSecurityConfigurerAdapter {

    /**
     * The constant AUTHORIZATION_HEADER.
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * The constant REFRESH_TOKEN.
     */
    public static final String REFRESH_TOKEN = "refresh_token";

    /**
     * The constant AUTHORIZATION_TOKEN.
     */
    public static final String AUTHORIZATION_TOKEN = "access_token";

    /**
     * The constant ACCESS_TOKEN_NEAR_EXPIRATION.
     */
    public static final String ACCESS_TOKEN_NEAR_EXPIRATION = "Access_token_near_expiration";

    /**
     * The constant SECURITY_IGNORE_URLS_SPILT_CHAR.
     */
    public static final String SECURITY_IGNORE_URLS_SPILT_CHAR = ",";

    /**
     * The constant TOKEN_PREFIX.
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    @Autowired
    @Qualifier("consoleUserDetailsService")
    private CustomUserDetailsServiceImpl userDetailsService;

    @Autowired
    @Qualifier("consoleJwtAuthenticationEntryPoint")
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @Autowired
    @Qualifier("consoleJwtTokenUtils")
    private JwtTokenUtils tokenProvider;

    @Autowired
    private Environment env;

    @Bean("consoleAuthenticationManager")
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Override
    public void configure(WebSecurity web) {
        String ignoreURLs = env.getProperty("console.ignore.urls", "/**");
        for (String ignoreURL : ignoreURLs.trim().split(SECURITY_IGNORE_URLS_SPILT_CHAR)) {
            web.ignoring().antMatchers(ignoreURL.trim());
        }
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        String csrfIgnoreUrls = env.getProperty("seata.security.csrf-ignore-urls");
        CsrfConfigurer<HttpSecurity> csrf = http.authorizeRequests().anyRequest().authenticated().and()
            // custom token authorize exception handler
            .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
            // since we use jwt, session is not necessary
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).disable().csrf();
        if (StringUtils.isNotBlank(csrfIgnoreUrls)) {
            csrf.ignoringAntMatchers(csrfIgnoreUrls.trim().split(SECURITY_IGNORE_URLS_SPILT_CHAR));
        }
        csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
        // don't disable csrf, jwt may be implemented based on cookies
        http.antMatcher("/api/v1/**").addFilterBefore(new ConsoleAuthenticationTokenFilter(tokenProvider),
                UsernamePasswordAuthenticationFilter.class);

        // disable cache
        http.headers().cacheControl();
    }

    /**
     * Password encoder password encoder.
     *
     * @return the password encoder
     */
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
