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
package org.apache.seata.mcp.service;

import org.apache.seata.common.result.SingleResult;
import org.apache.seata.console.utils.JwtTokenUtils;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;
import org.apache.seata.mcp.service.impl.MCPRPCServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MCPRPCServiceTest {

    @Mock
    private Environment env;

    @Mock
    private JwtTokenUtils jwtTokenUtils;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private MCPRPCServiceImpl service;

    private Object invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method method = MCPRPCServiceImpl.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(service, args);
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = MCPRPCServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, value);
    }

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(env.getProperty("server.port", "8081")).thenReturn("8081");
        lenient().when(env.getProperty("console.user.username", "seata")).thenReturn("seata");
        lenient().when(env.getProperty("console.user.password", "seata")).thenReturn("seata");
        lenient().when(jwtTokenUtils.validateToken(anyString())).thenReturn(true);
        lenient().when(jwtTokenUtils.createToken(any())).thenReturn("test-token");

        service.init();
        setField("restTemplate", restTemplate);
        setField("token", "Bearer test-token");
        setField("originJwt", "test-token");
    }

    @Test
    void testGetToken() {
        when(jwtTokenUtils.createToken(any())).thenReturn("new-token");
        service.getToken();
        verify(jwtTokenUtils).createToken(any());
    }

    @Test
    void testGetTCNameSpaces() {
        ResponseEntity<String> response = new ResponseEntity<>("{\"data\":\"ns1,ns2\"}", HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        SingleResult<?> result = service.getTCNameSpaces();
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    void testGetTCNameSpacesWithInvalidJson() {
        ResponseEntity<String> response = new ResponseEntity<>("invalid", HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        SingleResult<?> result = service.getTCNameSpaces();
        Assertions.assertFalse(result.isSuccess());
    }

    @Test
    void testSetNamespaceHeaderAndPathParam() {
        NameSpaceDetail detail = new NameSpaceDetail();
        detail.setNamespace("test-ns");
        detail.setCluster("cluster1");
        HttpHeaders headers = new HttpHeaders();

        service.setNamespaceHeaderAndPathParam(detail, headers, new HashMap<>());

        assertEquals("test-ns", headers.getFirst("x-seata-namespace"));
        assertEquals("cluster1", headers.getFirst("x-seata-cluster"));
    }

    @Test
    void testGetCallTC() {
        NameSpaceDetail detail = new NameSpaceDetail();
        detail.setNamespace("test-ns");
        detail.setCluster("test-clu");
        ResponseEntity<String> response = new ResponseEntity<>("{\"ok\":true}", HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        String result = service.getCallTC(detail, "/api/test", null, null, null);
        assertEquals("{\"ok\":true}", result);
    }

    @Test
    void testGetCallTCWithInvalidNamespace() {
        String result = service.getCallTC(null, "/api/test", null, null, null);
        assertTrue(result.contains("namespace"));
    }

    @Test
    void testDeleteCallTC() {
        NameSpaceDetail detail = new NameSpaceDetail();
        detail.setNamespace("test-ns");
        detail.setCluster("test-clu");
        ResponseEntity<String> response = new ResponseEntity<>("{\"ok\":true}", HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        String result = service.deleteCallTC(detail, "/api/test", null, null, null);
        assertEquals("{\"ok\":true}", result);
    }

    @Test
    void testDeleteCallTCWithInvalidNamespace() {
        String result = service.deleteCallTC(null, "/api/test", null, null, null);
        assertTrue(result.contains("namespace"));
    }

    @Test
    void testPutCallTC() {
        NameSpaceDetail detail = new NameSpaceDetail();
        detail.setNamespace("test-ns");
        detail.setCluster("test-clu");
        ResponseEntity<String> response = new ResponseEntity<>("{\"ok\":true}", HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        String result = service.putCallTC(detail, "/api/test", null, null, null);
        assertEquals("{\"ok\":true}", result);
    }

    @Test
    void testPutCallTCWithInvalidNamespace() {
        String result = service.putCallTC(null, "/api/test", null, null, null);
        assertTrue(result.contains("namespace"));
    }

    @Test
    void testGetCallTCLogsWithInvalidNamespace() {
        Mono<Void> result = service.getCallTCLogs(null, "/api/logs", null, null, null, "/tmp/test.log");
        StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();
    }

    @Test
    void testGetCallTCLogsWithValidNamespace() {
        NameSpaceDetail detail = new NameSpaceDetail();
        detail.setNamespace("test-ns");
        detail.setCluster("test-cluster");

        Mono<Void> result = service.getCallTCLogs(
                detail, "/api/logs", null, null, null, System.getProperty("java.io.tmpdir") + "/test-logs.txt");

        assertNotNull(result);
    }

    @Test
    void testObjectToQueryParamMap() throws Exception {
        Map<String, Object> result = (Map<String, Object>)
                invokePrivate("objectToQueryParamMap", new Class<?>[] {Object.class}, new Object[] {null});
        assertTrue(result.isEmpty());

        Map<String, Object> input = new HashMap<>();
        input.put("key", "value");
        result = (Map<String, Object>) invokePrivate("objectToQueryParamMap", new Class<?>[] {Object.class}, input);
        assertEquals("value", result.get("key"));

        TestParam param = new TestParam();
        param.name = "test";
        result = (Map<String, Object>) invokePrivate("objectToQueryParamMap", new Class<?>[] {Object.class}, param);
        assertEquals("test", result.get("name"));
    }

    @Test
    void testBuildUrl() throws Exception {
        String result = (String) invokePrivate(
                "buildUrl",
                new Class<?>[] {String.class, String.class, Map.class, Map.class},
                "http://localhost:8081",
                "/api",
                null,
                null);
        assertEquals("http://localhost:8081/api", result);

        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "123");
        result = (String) invokePrivate(
                "buildUrl",
                new Class<?>[] {String.class, String.class, Map.class, Map.class},
                "http://localhost:8081",
                "/api",
                pathParams,
                null);
        assertTrue(result.contains("id=123"));

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("name", "test");
        result = (String) invokePrivate(
                "buildUrl",
                new Class<?>[] {String.class, String.class, Map.class, Map.class},
                "http://localhost:8081",
                "/api",
                null,
                queryParams);
        assertTrue(result.contains("name=test"));
    }

    @Test
    void testBuildUrlWithArrayAndList() throws Exception {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("ids", new Integer[] {1, 2});
        String result = (String) invokePrivate(
                "buildUrl",
                new Class<?>[] {String.class, String.class, Map.class, Map.class},
                "http://localhost:8081",
                "/api",
                null,
                queryParams);
        assertTrue(result.contains("ids=1"));
        assertTrue(result.contains("ids=2"));

        queryParams.clear();
        List<String> tags = new ArrayList<>();
        tags.add("a");
        tags.add("b");
        queryParams.put("tags", tags);
        result = (String) invokePrivate(
                "buildUrl",
                new Class<?>[] {String.class, String.class, Map.class, Map.class},
                "http://localhost:8081",
                "/api",
                null,
                queryParams);
        assertTrue(result.contains("tags=a"));
        assertTrue(result.contains("tags=b"));
    }

    public static class TestParam {
        public String name;
    }
}
