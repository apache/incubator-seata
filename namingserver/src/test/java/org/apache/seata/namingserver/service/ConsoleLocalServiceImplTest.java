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
package org.apache.seata.namingserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.metadata.ClusterRole;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.namingserver.NamingServerNode;
import org.apache.seata.mcp.core.props.NameSpaceDetail;
import org.apache.seata.mcp.exception.ServiceCallException;
import org.apache.seata.namingserver.manager.NamingManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;

import static org.apache.seata.common.Constants.RAFT_GROUP_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ConsoleLocalServiceImplTest {

    private NamingManager namingManager;

    private MockRestServiceServer server;

    private ConsoleLocalServiceImpl consoleLocalService;

    @BeforeEach
    void setUp() {
        namingManager = mock(NamingManager.class);
        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        consoleLocalService = new ConsoleLocalServiceImpl(namingManager, restClientBuilder.build(), new ObjectMapper());
    }

    @Test
    void getCallTCShouldForwardRequestToLeaderNode() {
        NameSpaceDetail nameSpaceDetail = new NameSpaceDetail();
        nameSpaceDetail.setNamespace("public");
        nameSpaceDetail.setCluster("default");

        NamingServerNode leaderNode = new NamingServerNode();
        leaderNode.setRole(ClusterRole.LEADER);
        leaderNode.setUnit("unit-a");
        leaderNode.setControl(new Node.Endpoint("127.0.0.1", 7091, "http"));
        when(namingManager.getInstances("public", "default")).thenReturn(List.of(leaderNode));

        HttpHeaders headers = new HttpHeaders();
        headers.add("x-extra", "value");
        HashMap<String, String> queryParams = new HashMap<>();
        queryParams.put("page", "1");

        server.expect(once(), requestTo("http://127.0.0.1:7091/api/v1/console/sessions?page=1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("x-extra", "value"))
                .andExpect(header(RAFT_GROUP_HEADER, "unit-a"))
                .andRespond(withSuccess("local-ok", org.springframework.http.MediaType.TEXT_PLAIN));

        String response =
                consoleLocalService.getCallTC(nameSpaceDetail, "/api/v1/console/sessions", null, queryParams, headers);

        server.verify();
        assertEquals("local-ok", response);
    }

    @Test
    void getCallTCWithVgroupShouldForwardToNode() {
        NameSpaceDetail nameSpaceDetail = new NameSpaceDetail();
        nameSpaceDetail.setNamespace("public");
        nameSpaceDetail.setvGroup("vgroup-local");

        NamingServerNode vgroupNode = new NamingServerNode();
        vgroupNode.setControl(new Node.Endpoint("127.0.0.1", 8082, "http"));
        when(namingManager.getInstancesByVgroupAndNamespace("public", "vgroup-local", true))
                .thenReturn(List.of(vgroupNode));

        HttpHeaders headers = new HttpHeaders();
        HashMap<String, String> queryParams = new HashMap<>();

        server.expect(once(), requestTo("http://127.0.0.1:8082/api/v1/console/sessions"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("vgroup-ok", org.springframework.http.MediaType.TEXT_PLAIN));

        String response =
                consoleLocalService.getCallTC(nameSpaceDetail, "/api/v1/console/sessions", null, queryParams, headers);

        server.verify();
        assertEquals("vgroup-ok", response);
    }

    @Test
    void deleteCallTCShouldThrowWhenUpstreamReturnsNon2xx() {
        NameSpaceDetail nameSpaceDetail = new NameSpaceDetail();
        nameSpaceDetail.setNamespace("public");
        nameSpaceDetail.setCluster("default");

        NamingServerNode node = new NamingServerNode();
        node.setControl(new Node.Endpoint("127.0.0.1", 7091, "http"));
        when(namingManager.getInstances("public", "default")).thenReturn(List.of(node));

        server.expect(once(), requestTo("http://127.0.0.1:7091/api/v1/console/sessions"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("failed"));

        assertThrows(
                ServiceCallException.class,
                () -> consoleLocalService.deleteCallTC(
                        nameSpaceDetail, "/api/v1/console/sessions", null, new HashMap<>(), new HttpHeaders()));
        server.verify();
    }
}
