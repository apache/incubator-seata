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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.rm.RMClientAT;
import com.alibaba.fescar.test.common.ApplicationKeeper;
import com.alibaba.fescar.tm.dubbo.AccountService;
import com.alibaba.fescar.tm.dubbo.Order;
import com.alibaba.fescar.tm.dubbo.OrderService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * Please add the follow VM arguments:
 * <pre>
 *     -Djava.net.preferIPv4Stack=true
 * </pre>
 */
public class OrderServiceImpl implements OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);

    private AccountService accountService;

    private JdbcTemplate jdbcTemplate;

    @Override
    public Order create(String userId, String commodityCode, int orderCount) {
        LOGGER.info("Order Service Begin ... xid: " + RootContext.getXID());

        // 计算订单金额
        int orderMoney = calculate(commodityCode, orderCount);

        // 从账户余额扣款
        accountService.debit(userId, orderMoney);

        final Order order = new Order();
        order.userId = userId;
        order.commodityCode = commodityCode;
        order.count = orderCount;
        order.money = orderMoney;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(new PreparedStatementCreator() {

            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement pst = con.prepareStatement(
                        "insert into order_tbl (user_id, commodity_code, count, money) values (?, ?, ?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS);
                pst.setObject(1, order.userId);
                pst.setObject(2, order.commodityCode);
                pst.setObject(3, order.count);
                pst.setObject(4, order.money);
                return pst;
            }
        }, keyHolder);

        order.id = (long) keyHolder.getKey();

        LOGGER.info("Order Service End ... Created " + order);

        return order;
    }

    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private int calculate(String commodityId, int orderCount) {
        return 200 * orderCount;
    }

    public static void main(String[] args) throws Throwable {

        String applicationId = "dubbo-demo-order-service";
        String txServiceGroup = "my_test_tx_group";

        RMClientAT.init(applicationId, txServiceGroup);

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"dubbo-order-service.xml"});
        context.getBean("service");
        JdbcTemplate jdbcTemplate = (JdbcTemplate) context.getBean("jdbcTemplate");
        new ApplicationKeeper(context).keep();
    }
}
