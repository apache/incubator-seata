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
package org.apache.seata.discovery.registry.raft;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.metadata.MetadataResponse;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.util.*;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class RaftRegistryServiceImplTest {


    @BeforeAll
    public static void beforeClass() {
        System.setProperty("service.vgroupMapping.tx", "default");
        System.setProperty("registry.raft.username", "seata");
        System.setProperty("registry.raft.password", "seata");
        System.setProperty("registry.raft.serverAddr", "127.0.0.1:8092");
        System.setProperty("registry.raft.tokenValidityInMilliseconds", "10000");
        System.setProperty("registry.preferredNetworks", "10.10.*");
        ConfigurationFactory.getInstance();
    }

    @AfterAll
    public static void adAfterClass() throws Exception {
        System.clearProperty("service.vgroupMapping.tx");
    }

    /**
     * test whether throws exception when login failed
     */
    @Test
    public void testLoginFailed() throws IOException, NoSuchMethodException {
        String jwtToken = "null";
        String responseBody = "{\"code\":\"401\",\"message\":\"Login failed\",\"data\":\"" + jwtToken + "\",\"success\":false}";

        try (MockedStatic<HttpClientUtil> mockedStatic = Mockito.mockStatic(HttpClientUtil.class)) {

            CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
            StatusLine mockStatusLine = mock(StatusLine.class);

            when(mockResponse.getEntity()).thenReturn(new StringEntity(responseBody));
            when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
            when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);


            when(HttpClientUtil.doPost(any(String.class), any(Map.class), any(Map.class), any(int.class)))
                .thenReturn(mockResponse);

            // Use reflection to access and invoke the private method
            Method refreshTokenMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("refreshToken", String.class);
            refreshTokenMethod.setAccessible(true);
            assertThrows(Exception.class, () -> refreshTokenMethod.invoke(RaftRegistryServiceImpl.getInstance(), "127.0.0.1:8092"));

        }
    }

    /**
     * test whether the jwtToken updated when refreshToken method invoked
     */

    @Test
    public void testRefreshTokenSuccess() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        String jwtToken = "newToken";
        String responseBody = "{\"code\":\"200\",\"message\":\"success\",\"data\":\"" + jwtToken + "\",\"success\":true}";

        try (MockedStatic<HttpClientUtil> mockedStatic = Mockito.mockStatic(HttpClientUtil.class)) {

            CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
            StatusLine mockStatusLine = mock(StatusLine.class);

            when(mockResponse.getEntity()).thenReturn(new StringEntity(responseBody));
            when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
            when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);


            when(HttpClientUtil.doPost(any(String.class), any(Map.class), any(Map.class), any(int.class)))
                .thenReturn(mockResponse);


            Method refreshTokenMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("refreshToken", String.class);
            refreshTokenMethod.setAccessible(true);
            refreshTokenMethod.invoke(RaftRegistryServiceImpl.getInstance(), "127.0.0.1:8092");
            Field jwtTokenField = RaftRegistryServiceImpl.class.getDeclaredField("jwtToken");
            jwtTokenField.setAccessible(true);
            String jwtTokenAct = (String) jwtTokenField.get(null);


            assertEquals(jwtToken, jwtTokenAct);

        }
    }


    /**
     * test whether the jwtToken refreshed when it is expired
     */

    @Test
    public void testSecureTTL() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException, InterruptedException {
        Field tokenTimeStamp = RaftRegistryServiceImpl.class.getDeclaredField("tokenTimeStamp");
        tokenTimeStamp.setAccessible(true);
        tokenTimeStamp.setLong(RaftRegistryServiceImpl.class, System.currentTimeMillis());
        Method isExpiredMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("isTokenExpired");
        isExpiredMethod.setAccessible(true);
        boolean rst = (boolean) isExpiredMethod.invoke(null);
        assertEquals(false, rst);
        Thread.sleep(10000);
        rst = (boolean) isExpiredMethod.invoke(null);
        assertEquals(true, rst);
    }

    /**
     * RaftRegistryServiceImpl#controlEndpointStr()
     * RaftRegistryServiceImpl#transactionEndpointStr()
     */
    @Test
    public void testSelectEndpoint() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String jsonString = "{\"nodes\":[{\"control\":{\"host\":\"v-0.svc-l.default.svc.cluster.local\",\"port\":7091},\"transaction\":{\"host\":\"v-0.svc-l.default.svc.cluster.local\",\"port\":8091},\"internal\":{\"host\":\"v-0.svc-l.default.svc.cluster.local\",\"port\":9091},\"group\":\"default\",\"role\":\"LEADER\",\"version\":\"2.3.0-SNAPSHOT\",\"metadata\":{\"external\":[{\"host\":\"192.168.105.7\",\"controlPort\":30071,\"transactionPort\":30091},{\"host\":\"10.10.105.7\",\"controlPort\":30071,\"transactionPort\":30091}]}},{\"control\":{\"host\":\"v-2.svc-l.default.svc.cluster.local\",\"port\":7091},\"transaction\":{\"host\":\"v-2.svc-l.default.svc.cluster.local\",\"port\":8091},\"internal\":{\"host\":\"v-2.svc-l.default.svc.cluster.local\",\"port\":9091},\"group\":\"default\",\"role\":\"FOLLOWER\",\"version\":\"2.3.0-SNAPSHOT\",\"metadata\":{\"external\":[{\"host\":\"192.168.105.7\",\"controlPort\":30073,\"transactionPort\":30093},{\"host\":\"10.10.105.7\",\"controlPort\":30073,\"transactionPort\":30093}]}},{\"control\":{\"host\":\"v-1.svc-l.default.svc.cluster.local\",\"port\":7091},\"transaction\":{\"host\":\"v-1.svc-l.default.svc.cluster.local\",\"port\":8091},\"internal\":{\"host\":\"v-1.svc-l.default.svc.cluster.local\",\"port\":9091},\"group\":\"default\",\"role\":\"FOLLOWER\",\"version\":\"2.3.0-SNAPSHOT\",\"metadata\":{\"external\":[{\"host\":\"192.168.105.7\",\"controlPort\":30072,\"transactionPort\":30092},{\"host\":\"10.10.105.7\",\"controlPort\":30072,\"transactionPort\":30092}]}}],\"storeMode\":\"raft\",\"term\":1}";

        Method selectControlEndpointStrMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("selectControlEndpointStr", Node.class);
        selectControlEndpointStrMethod.setAccessible(true);
        Method selectTransactionEndpointStrMethod = RaftRegistryServiceImpl.class.getDeclaredMethod("selectTransactionEndpointStr", Node.class);
        selectTransactionEndpointStrMethod.setAccessible(true);

        ObjectMapper objectMapper = new ObjectMapper();
        MetadataResponse metadataResponse = objectMapper.readValue(jsonString, MetadataResponse.class);
        List<Node> nodes = metadataResponse.getNodes();

        for (Node node : nodes) {
            String controlEndpointStr = (String) selectControlEndpointStrMethod.invoke(null, node);;
            String transactionEndpointStr = (String) selectTransactionEndpointStrMethod.invoke(null, node);;
            Assertions.assertTrue(controlEndpointStr.contains("10.10.105.7:3007"));
            Assertions.assertTrue(transactionEndpointStr.contains("10.10.105.7:3009"));
        }
    }
}
