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

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityAutoConfigurationTest {

    @Test
    void glob_to_regex_translates_star() {
        Pattern p = SecurityAutoConfiguration.globToRegex("tenant_a_*");
        assertTrue(p.matcher("tenant_a_group").matches());
        assertTrue(p.matcher("tenant_a_").matches());
        assertFalse(p.matcher("tenant_b_group").matches());
    }

    @Test
    void glob_to_regex_translates_question_mark() {
        Pattern p = SecurityAutoConfiguration.globToRegex("g?");
        assertTrue(p.matcher("g1").matches());
        assertFalse(p.matcher("g12").matches());
    }

    @Test
    void glob_to_regex_escapes_regex_special_chars() {
        // A literal '.' in a glob must not match arbitrary characters.
        Pattern p = SecurityAutoConfiguration.globToRegex("group.a");
        assertTrue(p.matcher("group.a").matches());
        assertFalse(p.matcher("groupXa").matches(), "'.' must be treated literally, not as regex any-char");
    }

    @Test
    void glob_to_regex_supports_leading_wildcard() {
        Pattern p = SecurityAutoConfiguration.globToRegex("*_prod");
        assertTrue(p.matcher("a_prod").matches());
        assertTrue(p.matcher("_prod").matches());
        assertFalse(p.matcher("prod").matches());
    }

    @org.junit.jupiter.api.Test
    void omittedPermissionsLoadAsDenyAll() {
        SecurityProperties props = new SecurityProperties();
        SecurityProperties.ClusterConfig cfg = new SecurityProperties.ClusterConfig();
        cfg.setId("empty");
        cfg.setSecretRef("test-secret");
        props.setClusters(java.util.Collections.singletonList(cfg));
        SecretResolver resolver = org.mockito.Mockito.mock(SecretResolver.class);
        org.mockito.Mockito.when(resolver.resolve("test-secret")).thenReturn(new byte[32]);
        org.junit.jupiter.api.Assertions.assertFalse(new SecurityAutoConfiguration()
                .clusterIdentityRegistry(props, resolver)
                .find("empty")
                .get()
                .hasPermission(Permission.REGISTER));
    }

    @org.junit.jupiter.api.Test
    void legacyBypassValidatesJwtRatherThanTrustingAuthorizationHeader() throws Exception {
        SecurityProperties props = new SecurityProperties();
        props.setMode(SecurityProperties.Mode.ENFORCE);
        org.apache.seata.console.utils.JwtTokenUtils tokens =
                org.mockito.Mockito.mock(org.apache.seata.console.utils.JwtTokenUtils.class);
        org.mockito.Mockito.when(tokens.validateToken("valid")).thenReturn(true);
        jakarta.servlet.Filter filter = new SecurityAutoConfiguration()
                .seataSecurityFilter(
                        props,
                        new ClusterIdentityRegistry(),
                        new org.apache.seata.common.security.SignatureVerifier(
                                new org.apache.seata.common.security.NonceCache.InMemory(60000, () -> 1000L),
                                60000,
                                () -> 1000L),
                        new PermissionChecker(),
                        tokens)
                .getFilter();
        for (String token : new String[] {"valid", "invalid"}) {
            org.springframework.mock.web.MockHttpServletRequest request =
                    new org.springframework.mock.web.MockHttpServletRequest("POST", "/naming/v1/register");
            request.addHeader("Authorization", "Bearer " + token);
            java.util.concurrent.atomic.AtomicBoolean called = new java.util.concurrent.atomic.AtomicBoolean();
            filter.doFilter(
                    request,
                    new org.springframework.mock.web.MockHttpServletResponse(),
                    (req, resp) -> called.set(true));
            org.junit.jupiter.api.Assertions.assertEquals("valid".equals(token), called.get());
        }
    }
}
