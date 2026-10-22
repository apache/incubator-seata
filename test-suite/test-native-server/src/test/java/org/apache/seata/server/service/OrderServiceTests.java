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
package org.apache.seata.server.service;

import org.apache.seata.server.dto.OrderRequest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class OrderServiceTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceTests.class);

    @Autowired
    private OrderService orderService;

    @Test
    void order() {
        String commodityCode = "A001";
        String userId = "U001";
        long count1 = orderService.count(commodityCode);

        long count = 1;
        long money = 1;
        OrderRequest request = new OrderRequest();
        request.setUserId(userId);
        request.setCommodityCode(commodityCode);
        request.setCount(count);
        request.setMoney(money);

        orderService.order(request);

        long count2 = orderService.count(commodityCode);
        assertEquals(count1 + 1, count2);
    }
}
