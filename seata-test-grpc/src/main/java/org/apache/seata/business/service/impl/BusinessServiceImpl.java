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
package org.apache.seata.business.service.impl;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.seata.business.service.BusinessService;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.order.service.OrderService;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.apache.seata.storage.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class BusinessServiceImpl implements BusinessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessService.class);

    @DubboReference
    private StorageService storageService;
    @DubboReference
    private OrderService orderService;
    private final Random random = new Random();

    @Override
    @GlobalTransactional(timeoutMills = 300000, name = "spring-dubbo-tx")
    public void purchase(String userId, String commodityCode, int orderCount) {
        LOGGER.info("purchase begin ... xid: " + RootContext.getXID());
        storageService.deduct(commodityCode, orderCount);
        // just test batch update
        //stockS  ervice.batchDeduct(commodityCode, orderCount);
        orderService.create(userId, commodityCode, orderCount);
//        if (random.nextBoolean()) {
//            throw new RuntimeException("random exception mock!");
//        }
    }
}