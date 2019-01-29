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
import com.alibaba.fescar.test.common.ApplicationKeeper;
import com.alibaba.fescar.tm.dubbo.AccountService;
import com.alibaba.fescar.tm.dubbo.OrderService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Please add the follow VM arguments:
 * <pre>
 *     -Djava.net.preferIPv4Stack=true
 * </pre>
 */
public class AccountServiceImpl implements AccountService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountService.class);

    private JdbcTemplate jdbcTemplate;

    /**
     * Sets jdbc template.
     *
     * @param jdbcTemplate the jdbc template
     */
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void debit(String userId, int money) {
        LOGGER.info("Account Service ... xid: " + RootContext.getXID());
        LOGGER.info("Deducting balance SQL: update account_tbl set money = money - {} where user_id = {}",money,userId);

        jdbcTemplate.update("update account_tbl set money = money - ? where user_id = ?", new Object[] {money, userId});
        LOGGER.info("Account Service End ... ");
    }

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"dubbo-account-service.xml"});
        context.getBean("service");
        JdbcTemplate jdbcTemplate = (JdbcTemplate) context.getBean("jdbcTemplate");
        jdbcTemplate.update("delete from account_tbl where user_id = 'U100001'");
        jdbcTemplate.update("insert into account_tbl(user_id, money) values ('U100001', 999)");

        new ApplicationKeeper(context).keep();
    }
}
