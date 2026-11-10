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
package org.apache.seata.mcp.service.impl;

import org.apache.seata.common.exception.AuthenticationFailedException;
import org.apache.seata.console.utils.JwtTokenUtils;
import org.apache.seata.mcp.core.props.NameSpaceDetail;
import org.apache.seata.mcp.core.props.NamingServerProperties;
import org.apache.seata.mcp.exception.ServiceCallException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RemoteFailureBehaviorTest {
    private final JwtTokenUtils tokens = mock(JwtTokenUtils.class);
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server =
            MockRestServiceServer.bindTo(builder).build();
    private final ConsoleRemoteServiceImpl service =
            new ConsoleRemoteServiceImpl(tokens, builder.build(), new ObjectMapper(), new NamingServerProperties());

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    private NameSpaceDetail namespace() {
        NameSpaceDetail namespace = new NameSpaceDetail();
        namespace.setNamespace("public");
        namespace.setCluster("default");
        return namespace;
    }

    private String invoke(String method, NameSpaceDetail namespace) {
        switch (method) {
            case "GET":
                return service.getCallTC(namespace, "/test", null, null, null);
            case "PUT":
                return service.putCallTC(namespace, "/test", null, null, null);
            case "DELETE":
                return service.deleteCallTC(namespace, "/test", null, null, null);
            default:
                return service.getCallNameSpace("/test");
        }
    }

    private void authenticate() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("alice", "jwt", List.of()));
        when(tokens.validateToken("jwt")).thenReturn(true);
    }

    @Test
    void missingUnauthenticatedAndInvalidTokensFailBeforeHttp() {
        assertThrows(AuthenticationFailedException.class, service::getToken);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("alice", "jwt"));
        assertThrows(AuthenticationFailedException.class, service::getToken);
        authenticate();
        when(tokens.validateToken("jwt")).thenReturn(false);
        assertThrows(AuthenticationFailedException.class, service::getToken);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void invalidNamespaceNeverCallsRemote(String method) {
        assertTrue(invoke(method, null).contains("specify the namespace first"));
        assertTrue(invoke(method, new NameSpaceDetail()).contains("specify the namespace first"));
        verifyNoInteractions(tokens);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE", "NAMESPACE"})
    void serverErrorsRetainStatus(String verb) {
        authenticate();
        server.expect(requestTo("http://127.0.0.1:8081/test"))
                .andExpect(method(HttpMethod.valueOf("NAMESPACE".equals(verb) ? "GET" : verb)))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("upstream failed"));
        ServiceCallException exception = assertThrows(ServiceCallException.class, () -> invoke(verb, namespace()));
        assertEquals(502, exception.toErrorResponse().get("httpStatus"));
        assertTrue(exception.getMessage().contains("upstream failed"));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE", "NAMESPACE"})
    void transportErrorsBecomeServiceErrors(String verb) {
        authenticate();
        server.expect(requestTo("http://127.0.0.1:8081/test")).andRespond(request -> {
            throw new IOException("connection reset");
        });
        ServiceCallException exception = assertThrows(ServiceCallException.class, () -> invoke(verb, namespace()));
        assertTrue(exception.getMessage().endsWith("Failed."));
        assertNull(exception.toErrorResponse().get("httpStatus"));
        server.verify();
    }

    @Test
    void namespaceSuccessAndOptionalHeaders() {
        authenticate();
        server.expect(requestTo("http://127.0.0.1:8081/test"))
                .andRespond(withSuccess("namespaces", MediaType.TEXT_PLAIN));
        assertEquals("namespaces", service.getCallNameSpace("/test"));
        server.verify();
        NameSpaceDetail namespace = new NameSpaceDetail();
        namespace.setNamespace("public");
        HttpHeaders headers = new HttpHeaders();
        service.setNamespaceHeaderAndQueryParam(namespace, headers, null);
        assertFalse(headers.containsHeader("x-seata-cluster"));
        namespace.setvGroup("group");
        service.setNamespaceHeaderAndQueryParam(namespace, new HttpHeaders(), null);
        Map<String, String> query = new HashMap<>();
        service.setNamespaceHeaderAndQueryParam(namespace, new HttpHeaders(), query);
        assertEquals(Map.of("vGroup", "group"), query);
    }
}
