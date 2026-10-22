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
package org.apache.seata.server.controller;

import org.apache.seata.core.context.RootContext;
import org.apache.seata.server.dto.AccountMoneyRequest;
import org.apache.seata.server.dto.OrderRequest;
import org.apache.seata.server.dto.SeataRequest;
import org.apache.seata.server.dto.StorageRequest;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/seata")
public class SeataRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataRestController.class);

    private static final RestTemplate REST_TEMPLATE = new RestTemplate();

    private ServerProperties serverProperties;

    @Autowired
    public void setServerProperties(ServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    /**
     * Test distributed transaction
     *
     * @param request request parameters
     */
    @GlobalTransactional
    @PostMapping
    public Map<String, Object> seata(@RequestBody SeataRequest request) {

        String xid = RootContext.getXID();
        LOGGER.info("Distributed transaction {}: {}", RootContext.KEY_XID, xid);
        if (xid == null) {
            throw new NullPointerException("xid is null");
        }

        Integer port = serverProperties.getPort();
        if (port == null) {
            port = 8080;
        }

        // Deduct balance
        {
            String url = "http://127.0.0.1:" + port + "/account/money";

            AccountMoneyRequest accountMoneyRequest = new AccountMoneyRequest();
            accountMoneyRequest.setUserId(request.getUserId());
            accountMoneyRequest.setMoney(request.getMoney());

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set(RootContext.KEY_XID, xid);
            HttpEntity<AccountMoneyRequest> httpEntity = new HttpEntity<>(accountMoneyRequest, httpHeaders);

            Map map = REST_TEMPLATE.postForObject(url, httpEntity, Map.class);
            LOGGER.info("Balance deduction result: {}", map);
        }

        // Deduct stock
        {
            String url = "http://127.0.0.1:" + port + "/storage";

            StorageRequest storageRequest = new StorageRequest();
            storageRequest.setCommodityCode(request.getCommodityCode());
            storageRequest.setCount(request.getCount());

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set(RootContext.KEY_XID, xid);
            HttpEntity<StorageRequest> httpEntity = new HttpEntity<>(storageRequest, httpHeaders);

            Map map = REST_TEMPLATE.postForObject(url, httpEntity, Map.class);
            LOGGER.info("Stock deduction result: {}", map);
        }

        // Create order
        {
            String url = "http://127.0.0.1:" + port + "/order";

            OrderRequest orderRequest = new OrderRequest();
            orderRequest.setUserId(request.getUserId());
            orderRequest.setCommodityCode(request.getCommodityCode());
            orderRequest.setCount(request.getCount());
            orderRequest.setMoney(request.getMoney());

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set(RootContext.KEY_XID, xid);
            HttpEntity<OrderRequest> httpEntity = new HttpEntity<>(orderRequest, httpHeaders);

            Map map = REST_TEMPLATE.postForObject(url, httpEntity, Map.class);
            LOGGER.info("Order creation result: {}", map);
        }

        return Map.of("code", 200);
    }
}
