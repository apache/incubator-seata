/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.seata.server.console.controller;

import io.seata.console.bean.NodeStatus;
import io.seata.console.result.SingleResult;
import io.seata.discovery.registry.MultiRegistryFactory;
import io.seata.discovery.registry.RegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Junduo Dong
 */
@RestController
@RequestMapping("/api/v1/admin/status")
public class StatusController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiRegistryFactory.class);

    private final String UNKNOWN_STATUS = "UNKNOWN";

    @Autowired
    private RestTemplate restTemplate;

    private String HTTP_PROTOCOL = "http://";

    private String URI_HEALTH = "/health";

    @Value("${server.port}")
    private int serverPort;

    @RequestMapping
    SingleResult<List<NodeStatus>> checkClusterHealth(@RequestHeader Map<String, String> headers){
        List<NodeStatus> statuses = new ArrayList<>();
        List<RegistryService> registryServices = MultiRegistryFactory.getInstances();
        for (RegistryService registryService: registryServices) {
            try {
                List<InetSocketAddress> inetSocketAddressList = registryService.getClusterNodes();
                inetSocketAddressList.forEach(address -> {
                    String ipAndPort = address.getAddress().getHostAddress() + ":" + serverPort;
                    String url = HTTP_PROTOCOL + ipAndPort + URI_HEALTH;
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.add("authorization", headers.get("authorization"));
                    HttpEntity<MultiValueMap<String, Object>> formEntity = new HttpEntity<>(httpHeaders);
                    try {
                        ResponseEntity<String> result = restTemplate.exchange(url, HttpMethod.GET, formEntity, String.class);
                        if (result.getStatusCodeValue() == 200) {
                            statuses.add(new NodeStatus(ipAndPort, result.getBody(), registryService.getType()));
                        } else {
                            statuses.add(new NodeStatus(ipAndPort, UNKNOWN_STATUS, registryService.getType()));
                        }
                    } catch (Exception e) {
                        statuses.add(new NodeStatus(ipAndPort, UNKNOWN_STATUS, registryService.getType()));
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Read registry {} information failed", registryService.getType());
            }
        }
        return SingleResult.success(statuses);
    }
}
