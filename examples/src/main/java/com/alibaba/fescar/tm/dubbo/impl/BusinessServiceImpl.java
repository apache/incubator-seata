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

package com.alibaba.fescar.tm.dubbo.impl;

import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.spring.annotation.GlobalTransactional;
import com.alibaba.fescar.tm.dubbo.BusinessService;
import com.alibaba.fescar.tm.dubbo.OrderService;
import com.alibaba.fescar.tm.dubbo.StorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Please add the follow VM arguments:
 * <pre>
 *     -Djava.net.preferIPv4Stack=true
 * </pre>
 */
public class BusinessServiceImpl implements BusinessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessService.class);

    private StorageService storageService;
    private OrderService orderService;

    @Override
    @GlobalTransactional(timeoutMills = 300000, name = "dubbo-demo-tx")
    public void purchase(String userId, String commodityCode, int orderCount) {
        storageService.deduct(commodityCode, orderCount);
        orderService.create(userId, commodityCode, orderCount);

    }

    public void setStorageService(StorageService storageService) {
        this.storageService = storageService;
    }

    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
            new String[] {"dubbo-business.xml"});
        final BusinessService business = (BusinessService)context.getBean("business");

        LOGGER.info("Main business begin ... xid: " + RootContext.getXID());
        business.purchase("U100001", "C00321", 2);
        LOGGER.info("Main business end ... xid: " + RootContext.getXID());
    }
}
