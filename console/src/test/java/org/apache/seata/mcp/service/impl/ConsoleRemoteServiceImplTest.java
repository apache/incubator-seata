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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.console.config.WebSecurityConfig;
import org.apache.seata.console.utils.JwtTokenUtils;
import org.apache.seata.mcp.core.props.NameSpaceDetail;
import org.apache.seata.mcp.core.props.NamingServerProperties;
import org.apache.seata.mcp.exception.ServiceCallException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SuppressWarnings("null")
class ConsoleRemoteServiceImplTest {

    private JwtTokenUtils jwtTokenUtils;

    private MockRestServiceServer server;

    private ConsoleRemoteServiceImpl consoleRemoteService;

    @BeforeEach
    void setUp() {
        jwtTokenUtils = mock(JwtTokenUtils.class);
        when(jwtTokenUtils.validateToken("jwt-token")).thenReturn(true);

        NamingServerProperties namingServerProperties = new NamingServerProperties();
        namingServerProperties.setProtocol("http");
        namingServerProperties.setAddr(List.of("127.0.0.1:8081"));

        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();
        consoleRemoteService =
                new ConsoleRemoteServiceImpl(jwtTokenUtils, restClient, new ObjectMapper(), namingServerProperties);

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("user", "jwt-token", List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCallTCShouldSendAuthorizationAndNamespaceHeaders() {
        NameSpaceDetail nameSpaceDetail = new NameSpaceDetail();
        nameSpaceDetail.setNamespace("public");
        nameSpaceDetail.setCluster("default");

        HttpHeaders headers = new HttpHeaders();
        headers.add("x-custom", "value");
        HashMap<String, String> queryParams = new HashMap<>();
        queryParams.put("page", "1");

        server.expect(once(), requestTo("http://127.0.0.1:8081/api/v1/console/sessions?page=1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(WebSecurityConfig.AUTHORIZATION_HEADER, WebSecurityConfig.TOKEN_PREFIX + "jwt-token"))
                .andExpect(header("x-seata-namespace", "public"))
                .andExpect(header("x-seata-cluster", "default"))
                .andExpect(header("x-custom", "value"))
                .andRespond(withSuccess("remote-ok", org.springframework.http.MediaType.TEXT_PLAIN));

        String response =
                consoleRemoteService.getCallTC(nameSpaceDetail, "/api/v1/console/sessions", null, queryParams, headers);

        server.verify();
        assertEquals("remote-ok", response);
    }

    @Test
    void putCallTCShouldEncodeVgroupAsQueryParameter() {
        NameSpaceDetail nameSpaceDetail = new NameSpaceDetail();
        nameSpaceDetail.setNamespace("public");
        nameSpaceDetail.setvGroup("vgroup-a");

        server.expect(once(), requestTo("http://127.0.0.1:8081/api/v1/console/sessions?vGroup=vgroup-a"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header(WebSecurityConfig.AUTHORIZATION_HEADER, WebSecurityConfig.TOKEN_PREFIX + "jwt-token"))
                .andExpect(header("x-seata-namespace", "public"))
                .andExpect(headerDoesNotExist("x-seata-cluster"))
                .andRespond(withSuccess("updated", org.springframework.http.MediaType.TEXT_PLAIN));

        String response = consoleRemoteService.putCallTC(
                nameSpaceDetail, "/api/v1/console/sessions", null, new HashMap<>(), new HttpHeaders());

        server.verify();
        assertEquals("updated", response);
    }

    @Test
    void deleteCallTCShouldSendHeadersAndReturnResponse() {
        NameSpaceDetail nameSpaceDetail = new NameSpaceDetail();
        nameSpaceDetail.setNamespace("public");
        nameSpaceDetail.setCluster("default");

        HttpHeaders headers = new HttpHeaders();
        headers.add("x-custom", "value");
        HashMap<String, String> queryParams = new HashMap<>();

        server.expect(once(), requestTo("http://127.0.0.1:8081/api/v1/console/sessions"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(WebSecurityConfig.AUTHORIZATION_HEADER, WebSecurityConfig.TOKEN_PREFIX + "jwt-token"))
                .andExpect(header("x-seata-namespace", "public"))
                .andExpect(header("x-seata-cluster", "default"))
                .andExpect(header("x-custom", "value"))
                .andRespond(withSuccess("deleted-ok", org.springframework.http.MediaType.TEXT_PLAIN));

        String response = consoleRemoteService.deleteCallTC(
                nameSpaceDetail, "/api/v1/console/sessions", null, queryParams, headers);

        server.verify();
        assertEquals("deleted-ok", response);
    }

    @Test
    void getCallNameSpaceShouldThrowWhenUpstreamReturnsNon2xx() {
        server.expect(once(), requestTo("http://127.0.0.1:8081/api/v1/console/namespaces"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(WebSecurityConfig.AUTHORIZATION_HEADER, WebSecurityConfig.TOKEN_PREFIX + "jwt-token"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("boom"));

        assertThrows(
                ServiceCallException.class, () -> consoleRemoteService.getCallNameSpace("/api/v1/console/namespaces"));
        server.verify();
    }
}
