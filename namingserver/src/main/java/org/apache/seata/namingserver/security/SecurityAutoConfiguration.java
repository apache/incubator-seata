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
package org.apache.seata.namingserver.security;

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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Wires up the security layer only when {@code seata.security.enabled=true}. When disabled
 * the beans are not created and the request pipeline behaves exactly as before.
 *
 * <p>Ordering: the {@link SecurityFilter} sits at {@link Ordered#HIGHEST_PRECEDENCE} so it
 * runs before {@link org.apache.seata.namingserver.filter.ConsoleRemotingFilter}. That way the
 * proxy never dispatches an unauthenticated request to TC.
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
@ConditionalOnProperty(prefix = "seata.security", name = "enabled", havingValue = "true")
public class SecurityAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public SecretResolver secretResolver() {
        return new SecretResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public ClusterIdentityRegistry clusterIdentityRegistry(SecurityProperties props, SecretResolver resolver) {
        ClusterIdentityRegistry registry = new ClusterIdentityRegistry();
        List<ClusterIdentity> loaded = new ArrayList<>();
        for (SecurityProperties.ClusterConfig cfg : props.getClusters()) {
            byte[] secret = resolver.resolve(cfg.getSecretRef());
            if (secret.length < HmacSigner.MIN_KEY_LENGTH_BYTES) {
                throw new IllegalStateException("secret for cluster-id '" + cfg.getId() + "' is shorter than "
                        + HmacSigner.MIN_KEY_LENGTH_BYTES + " bytes");
            }
            List<Pattern> patterns = cfg.getAllowedVgroups() == null
                    ? Collections.emptyList()
                    : cfg.getAllowedVgroups().stream()
                            .map(SecurityAutoConfiguration::globToRegex)
                            .collect(Collectors.toList());
            loaded.add(new ClusterIdentity(
                    cfg.getId(),
                    secret,
                    cfg.getAllowedNamespaces() == null
                            ? Collections.emptySet()
                            : new java.util.HashSet<>(cfg.getAllowedNamespaces()),
                    cfg.getAllowedClusters() == null
                            ? Collections.emptySet()
                            : new java.util.HashSet<>(cfg.getAllowedClusters()),
                    patterns,
                    cfg.getPermissions() == null
                            ? java.util.EnumSet.noneOf(Permission.class)
                            : java.util.EnumSet.copyOf(cfg.getPermissions())));
        }
        registry.reload(loaded);
        LOGGER.info("Loaded {} cluster identities for NamingServer security", registry.size());
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public NonceCache nonceCache(SecurityProperties props) {
        long ttlMillis = TimeUnit.MINUTES.toMillis(props.getNonceCacheMinutes());
        return new NonceCache.InMemory(ttlMillis, System::currentTimeMillis);
    }

    @Bean
    @ConditionalOnMissingBean
    public SignatureVerifier signatureVerifier(NonceCache cache, SecurityProperties props) {
        return new SignatureVerifier(
                cache, TimeUnit.SECONDS.toMillis(props.getReplayWindowSeconds()), System::currentTimeMillis);
    }

    @Bean
    @ConditionalOnMissingBean
    public PermissionChecker permissionChecker() {
        return new PermissionChecker();
    }

    @Bean
    public FilterRegistrationBean<Filter> seataSecurityFilter(
            SecurityProperties props,
            ClusterIdentityRegistry registry,
            SignatureVerifier verifier,
            PermissionChecker checker) {
        SecurityFilter filter = new SecurityFilter(props, registry, verifier, checker);
        FilterRegistrationBean<Filter> reg = new FilterRegistrationBean<>();
        reg.setFilter(filter);
        reg.addUrlPatterns("/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return reg;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "seata.security.outbound", name = "cluster-id")
    public OutboundSigner outboundSigner(SecurityProperties props, SecretResolver resolver) {
        SecurityProperties.OutboundConfig out = props.getOutbound();
        byte[] secret = resolver.resolve(out.getSecretRef());
        if (secret.length < HmacSigner.MIN_KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "outbound secret is shorter than " + HmacSigner.MIN_KEY_LENGTH_BYTES + " bytes");
        }
        return new OutboundSigner(out.getClusterId(), secret);
    }

    /**
     * Trivial glob → regex translator supporting {@code *} and {@code ?}. Handles nothing else,
     * on purpose: complex regex belongs in a dedicated pattern language, not a config file.
     */
    static Pattern globToRegex(String glob) {
        StringBuilder sb = new StringBuilder(glob.length() + 4);
        sb.append('^');
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*':
                    sb.append(".*");
                    break;
                case '?':
                    sb.append('.');
                    break;
                case '.':
                case '\\':
                case '+':
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                case '^':
                case '$':
                case '|':
                    sb.append('\\').append(c);
                    break;
                default:
                    sb.append(c);
            }
        }
        sb.append('$');
        return Pattern.compile(sb.toString());
    }
}
