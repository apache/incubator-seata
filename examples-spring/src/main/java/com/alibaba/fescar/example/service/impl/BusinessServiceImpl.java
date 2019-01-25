/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.example.service.impl;

import com.alibaba.fescar.spring.annotation.GlobalTransactional;
import com.alibaba.fescar.example.service.BusinessService;
import com.alibaba.fescar.example.service.rpc.OrderServiceRpc;
import com.alibaba.fescar.example.service.rpc.StorageServiceRpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BusinessServiceImpl implements BusinessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessService.class);

    @Autowired
    StorageServiceRpc storageService;

    @Autowired
    OrderServiceRpc orderService;

    @GlobalTransactional(timeoutMills = 30000, name = "spring-demo-tx")
    public void purchaseRollback(String userId, String commodityCode, int orderCount) {
        storageService.deduct(commodityCode, orderCount);
        orderService.create(userId, commodityCode, orderCount);
        LOGGER.info("purchaseRollback done.userId={},commodityCode={},orderCount={}"
                , userId, commodityCode, orderCount);
        throw new RuntimeException("transaction should be rollback");

    }

    @Override
    @GlobalTransactional(timeoutMills = 30000, name = "spring-demo-tx")
    public void purchase(String userId, String commodityCode, int orderCount) {
        storageService.deduct(commodityCode, orderCount);
        orderService.create(userId, commodityCode, orderCount);
        LOGGER.info("purchase done.userId={},commodityCode={},orderCount={}"
                , userId, commodityCode, orderCount);
    }

}
