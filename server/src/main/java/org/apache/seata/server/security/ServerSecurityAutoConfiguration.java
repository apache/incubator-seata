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
package org.apache.seata.server.security;

import jakarta.servlet.Filter;
import org.apache.seata.common.security.HmacSigner;
import org.apache.seata.common.security.NonceCache;
import org.apache.seata.common.security.SignatureVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Loads the TC-side inbound auth pipeline when
 * {@code seata.registry.seata.security.inbound.enabled=true}. Kept as a separate
 * {@code @Configuration} so operators can strip it out entirely by leaving the property unset.
 */
@Configuration
@EnableConfigurationProperties(ServerSecurityProperties.class)
@ConditionalOnProperty(prefix = "seata.registry.seata.security.inbound", name = "enabled", havingValue = "true")
public class ServerSecurityAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerSecurityAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public ServerSecretResolver serverSecretResolver() {
        return new ServerSecretResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public AllowedCallerRegistry allowedCallerRegistry(ServerSecurityProperties props, ServerSecretResolver resolver) {
        AllowedCallerRegistry registry = new AllowedCallerRegistry();
        List<AllowedCaller> loaded = new ArrayList<>();
        for (ServerSecurityProperties.CallerConfig cfg : props.getAllowedCallers()) {
            byte[] secret = resolver.resolve(cfg.getSecretRef());
            if (secret.length < HmacSigner.MIN_KEY_LENGTH_BYTES) {
                throw new IllegalStateException("secret for caller '" + cfg.getId() + "' shorter than "
                        + HmacSigner.MIN_KEY_LENGTH_BYTES + " bytes");
            }
            loaded.add(new AllowedCaller(
                    cfg.getId(),
                    secret,
                    cfg.getPermissions() == null
                            ? EnumSet.noneOf(CallerPermission.class)
                            : EnumSet.copyOf(cfg.getPermissions())));
        }
        registry.reload(loaded);
        LOGGER.info("[seata-server][security] loaded {} allowed caller(s)", registry.size());
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public NonceCache serverNonceCache(ServerSecurityProperties props) {
        return new NonceCache.InMemory(
                TimeUnit.MINUTES.toMillis(props.getNonceCacheMinutes()), System::currentTimeMillis);
    }

    @Bean
    @ConditionalOnMissingBean
    public SignatureVerifier serverSignatureVerifier(NonceCache serverNonceCache, ServerSecurityProperties props) {
        return new SignatureVerifier(
                serverNonceCache, TimeUnit.SECONDS.toMillis(props.getReplayWindowSeconds()), System::currentTimeMillis);
    }

    @Bean
    @ConditionalOnMissingBean
    public RouteAuthorizer routeAuthorizer() {
        return new RouteAuthorizer();
    }

    @Bean
    public FilterRegistrationBean<Filter> seataServerAuthFilter(
            ServerSecurityProperties props,
            AllowedCallerRegistry registry,
            SignatureVerifier serverSignatureVerifier,
            RouteAuthorizer authorizer) {
        SeataServerAuthFilter filter = new SeataServerAuthFilter(props, registry, serverSignatureVerifier, authorizer);
        FilterRegistrationBean<Filter> reg = new FilterRegistrationBean<>();
        reg.setFilter(filter);
        // Only the paths TC actually needs to protect — /vgroup/v1/*, /api/*/console/*.
        // Broader match is fine because the filter itself pass-throughs unknown routes.
        reg.addUrlPatterns("/vgroup/v1/*", "/api/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return reg;
    }
}
