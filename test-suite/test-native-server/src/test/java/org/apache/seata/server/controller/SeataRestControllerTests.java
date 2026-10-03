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

import org.apache.seata.server.dto.SeataRequest;
import org.apache.seata.server.service.AccountService;
import org.apache.seata.server.service.OrderService;
import org.apache.seata.server.service.StorageService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class SeataRestControllerTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeataRestControllerTests.class);

    @Autowired
    private AccountService accountService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private StorageService storageService;

    @Test
    void ok() {
        String userId = "U001";
        String commodityCode = "A001";
        long count = -2;
        long money = -1;

        Long money1 = accountService.getMoney(userId);
        long orderCount1 = orderService.count(commodityCode);
        long storageCount1 = storageService.count(commodityCode);

        {
            SeataRequest request = new SeataRequest();
            request.setCommodityCode(commodityCode);
            request.setUserId(userId);
            request.setCount(count);
            request.setMoney(money);

            HttpEntity<SeataRequest> httpEntity = new HttpEntity<>(request);

            String url = "http://127.0.0.1:50180/seata";
            String value = new RestTemplate().postForObject(url, httpEntity, String.class);
            LOGGER.info(value);
            assertEquals("{\"code\":200}", value);
        }

        Long money2 = accountService.getMoney(userId);
        long orderCount2 = orderService.count(commodityCode);
        long storageCount2 = storageService.count(commodityCode);

        assertEquals(money1 + money, money2);
        assertEquals(orderCount1 + 1, orderCount2);
        assertEquals(storageCount1 + count, storageCount2);
    }

    /**
     * Test distributed transaction: data rollback
     */
    @Test
    void error() {
        String userId = "U001";
        String commodityCode = "A001";

        // First: balance deduction succeeds
        long money = -1;

        // Second: stock deduction fails
        long count = -20000000;

        Long money1 = accountService.getMoney(userId);
        long orderCount1 = orderService.count(commodityCode);
        long storageCount1 = storageService.count(commodityCode);

        {
            SeataRequest request = new SeataRequest();
            request.setCommodityCode(commodityCode);
            request.setUserId(userId);
            request.setCount(count);
            request.setMoney(money);

            HttpEntity<SeataRequest> httpEntity = new HttpEntity<>(request);

            String url = "http://127.0.0.1:50180/seata";
            assertThrows(Exception.class, () -> {
                try {
                    new RestTemplate().postForObject(url, httpEntity, Map.class);
                } catch (Exception e) {
                    LOGGER.error("Distributed transaction exception: ", e);
                    String message = e.getMessage();
                    assertThat(message).doesNotContain("No instances available");
                    assertThat(message).doesNotContain("I/O error on POST request");
                    assertThat(message).contains("Insufficient stock");
                    throw e;
                }
            });
        }

        Long money2 = accountService.getMoney(userId);
        long orderCount2 = orderService.count(commodityCode);
        long storageCount2 = storageService.count(commodityCode);

        // Third: data rollback, database remains unchanged
        assertEquals(money1, money2);
        assertEquals(orderCount1, orderCount2);
        assertEquals(storageCount1, storageCount2);
    }
}
