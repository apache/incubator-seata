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
package org.apache.seata.server.common;

import org.apache.seata.core.model.PoolConfigUpdateRequest;
import org.apache.seata.core.rpc.processor.server.ConnectionPoolInfoCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client class for communicating with downstream services to notify connection pool configuration updates
 */
public class HttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClient.class);

    private static final String CLIENT_URL_PREFIX = "http://";

    private static final String UPDATE_CONNECTION_POOL_CONFIG_URL = "/client/pool/update";

    private final RestTemplate restTemplate;

    public HttpClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean notifyDownstreamService(String poolName, String clientUrl, PoolConfigUpdateRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            request.setPoolName(poolName);
            HttpEntity<PoolConfigUpdateRequest> requestEntity = new HttpEntity<>(request, headers);
            String url = CLIENT_URL_PREFIX + clientUrl + UPDATE_CONNECTION_POOL_CONFIG_URL;
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                LOGGER.info("Connection pool config updated successfully");
                ConnectionPoolInfoCache.getInstance().refresh(request);
                return true;
            }
            LOGGER.warn("Failed to update connection pool config.");
        } catch (Exception e) {
            LOGGER.error("Error while notifying downstream service: {}", e.getMessage());
        }
        return false;
    }
}
