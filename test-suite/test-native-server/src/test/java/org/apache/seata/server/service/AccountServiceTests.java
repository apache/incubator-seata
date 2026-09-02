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

import org.apache.seata.server.dto.AccountMoneyRequest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AccountServiceTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountServiceTests.class);

    @Autowired
    private AccountService accountService;

    @Test
    void getMoney() {
        String userId = "U001";
        Long money = accountService.getMoney(userId);
        assertNotNull(money);
    }

    @Test
    void money_1() {
        String userId = "U001";

        Long money1 = accountService.getMoney(userId);
        assertNotNull(money1);

        long money = 1;
        AccountMoneyRequest request = new AccountMoneyRequest();
        request.setUserId(userId);
        request.setMoney(money);

        accountService.money(request);

        Long money2 = accountService.getMoney(userId);
        assertNotNull(money2);

        assertEquals(money1 + money, money2);
    }

    @Test
    void money_2() {
        String userId = "U001";

        Long money1 = accountService.getMoney(userId);
        assertNotNull(money1);

        long money = -1;
        AccountMoneyRequest request = new AccountMoneyRequest();
        request.setUserId(userId);
        request.setMoney(money);
        accountService.money(request);

        Long money2 = accountService.getMoney(userId);
        assertNotNull(money2);

        assertEquals(money1 + money, money2);
    }

    @Test
    void money_3() {
        String userId = "U001";
        long money = -10000000;
        AccountMoneyRequest request = new AccountMoneyRequest();
        request.setUserId(userId);
        request.setMoney(money);
        assertThrows(RuntimeException.class, () -> {
            accountService.money(request);
        });
    }
}
