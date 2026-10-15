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
package org.apache.seata.console.security;

import io.jsonwebtoken.Jwts;
import org.apache.seata.console.controller.AuthController;
import org.apache.seata.console.controller.OverviewController;
import org.apache.seata.console.utils.JwtTokenUtils;
import org.apache.seata.mcp.service.impl.ModifyConfirmServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationBehaviorTest {
    private static final String SECRET = Base64.getEncoder()
            .encodeToString("test-only-signing-key-32-bytes-long".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private JwtTokenUtils tokens(long lifetime) {
        JwtTokenUtils tokens = new JwtTokenUtils();
        ReflectionTestUtils.setField(tokens, "secretKey", SECRET);
        ReflectionTestUtils.setField(tokens, "tokenValidityInMilliseconds", lifetime);
        return tokens;
    }

    @Test
    void tokenRoundTripAndInvalidTokens() {
        JwtTokenUtils tokens = tokens(60000);
        Authentication auth = new UsernamePasswordAuthenticationToken("alice", "password", List.of());
        String token = tokens.createToken(auth);
        assertTrue(tokens.validateToken(token));
        assertEquals("alice", tokens.getAuthentication(token).getName());
        assertEquals(token, tokens.getAuthentication(token).getCredentials());
        assertFalse(tokens.validateToken("invalid"));
        assertFalse(tokens.validateToken(""));
        assertFalse(tokens.validateToken(tokens(-60000).createToken(auth)));
        assertFalse(tokens.validateToken(Jwts.builder().setSubject("alice").compact()));
        JwtTokenUtils other = tokens(60000);
        ReflectionTestUtils.setField(
                other,
                "secretKey",
                Base64.getEncoder()
                        .encodeToString("different-test-key-32-bytes-long!!"
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertFalse(tokens.validateToken(other.createToken(auth)));
    }

    @Test
    void modificationKeyHasUserExpiryAndModifyClaim() {
        JwtTokenUtils tokens = tokens(60000);
        ModifyConfirmServiceImpl service = new ModifyConfirmServiceImpl(tokens);
        ReflectionTestUtils.setField(service, "secretKey", SECRET);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("alice", ""));
        long before = System.currentTimeMillis();
        String key = service.confirmAndGetKey().get("modify_key");
        io.jsonwebtoken.Claims claims =
                Jwts.parser().setSigningKey(SECRET).parseClaimsJws(key).getBody();
        assertEquals("alice", claims.getSubject());
        assertEquals("", claims.get("modify"));
        assertTrue(claims.getExpiration().getTime() >= before + 179000);
        assertTrue(service.isValidKey(key));
        assertFalse(service.isValidKey("bad"));
    }

    @Test
    void configuredAndGeneratedPasswordsAreEncodedAndUnknownUsersRejected() {
        for (String configured : List.of("password", "")) {
            CustomUserDetailsServiceImpl service = new CustomUserDetailsServiceImpl();
            ReflectionTestUtils.setField(service, "username", "alice");
            ReflectionTestUtils.setField(service, "password", configured);
            service.init();
            UserDetails details = service.loadUserByUsername("alice");
            String raw = (String) ReflectionTestUtils.getField(service, "password");
            assertTrue(new BCryptPasswordEncoder().matches(raw, details.getPassword()));
            if (configured.isEmpty()) {
                assertEquals(8, raw.length());
            }
            assertEquals("alice", details.getUsername());
            assertTrue(details.getAuthorities().isEmpty());
            assertTrue(details.isAccountNonExpired());
            assertTrue(details.isAccountNonLocked());
            assertTrue(details.isCredentialsNonExpired());
            assertTrue(details.isEnabled());
            assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("other"));
        }
    }

    @Test
    void loginReturnsTokenAndRejectsBadCredentials() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        JwtTokenUtils tokens = mock(JwtTokenUtils.class);
        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "authenticationManager", manager);
        ReflectionTestUtils.setField(controller, "jwtTokenUtils", tokens);
        Authentication auth = new UsernamePasswordAuthenticationToken("alice", "password", List.of());
        when(manager.authenticate(any())).thenReturn(auth);
        when(tokens.createToken(auth)).thenReturn("token");
        User user = new User("old", "old");
        user.setUsername("alice");
        user.setPassword("password");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertEquals("Bearer token", controller.login(response, user).getData());
        assertEquals("Bearer token", response.getHeader("Authorization"));
        assertSame(auth, SecurityContextHolder.getContext().getAuthentication());
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));
        assertNull(controller.login(new MockHttpServletResponse(), user).getData());
    }

    @Test
    void unauthorizedEntryPointAndOverviewContract() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new JwtAuthenticationEntryPoint().commence(null, response, new BadCredentialsException("test"));
        assertEquals(401, response.getStatus());
        assertEquals("Unauthorized", response.getErrorMessage());
        List<?> overview = new OverviewController().getData().getData();
        assertEquals(10, overview.size());
        assertEquals(Map.of("name", "seata9", "id", 9), overview.get(0));
        assertEquals(Map.of("name", "seata0", "id", 0), overview.get(9));
    }
}
