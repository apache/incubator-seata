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
package org.apache.seata.server.service.impl;

import org.apache.seata.server.dao.OrderDAO;
import org.apache.seata.server.dto.OrderRequest;
import org.apache.seata.server.entity.Order;
import org.apache.seata.server.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

    private OrderDAO orderDAO;

    @Autowired
    public void setOrderDAO(OrderDAO orderDAO) {
        this.orderDAO = orderDAO;
    }

    @Override
    public long count(String commodityCode) {
        return orderDAO.countByCommodityCode(commodityCode);
    }

    @Override
    @Transactional
    public void order(OrderRequest request) {
        Long count = request.getCount();
        Long money = request.getMoney();
        String commodityCode = request.getCommodityCode();
        String userId = request.getUserId();
        if (count == null) {
            throw new RuntimeException("Order count must not be null");
        }
        if (money == null) {
            throw new RuntimeException("Order amount must not be null");
        }
        count = Math.abs(count);
        money = Math.abs(money);
        Order order = new Order(userId, commodityCode, count.intValue(), money.intValue());
        orderDAO.save(order);
    }
}
