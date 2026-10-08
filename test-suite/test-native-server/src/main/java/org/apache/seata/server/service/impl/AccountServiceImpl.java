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

import org.apache.seata.server.dao.AccountDAO;
import org.apache.seata.server.dto.AccountMoneyRequest;
import org.apache.seata.server.entity.Account;
import org.apache.seata.server.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountServiceImpl implements AccountService {

    private AccountDAO accountDAO;

    @Autowired
    public void setAccountDAO(AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
    }

    @Override
    public Long getMoney(String userId) {
        Account account = accountDAO.findByUserId(userId);
        if (account == null) {
            return null;
        }
        return account.getMoney();
    }

    @Override
    @Transactional
    public void money(AccountMoneyRequest request) {
        String userId = request.getUserId();
        Long money = request.getMoney();

        if (money == null) {
            return;
        }

        if (money == 0) {
            return;
        }

        Account account = accountDAO.findByUserId(userId);
        if (account == null) {
            throw new RuntimeException("User does not exist");
        }

        if (money > 0) {
            account.setMoney(account.getMoney() + money);
        } else {
            if (account.getMoney() + money < 0) {
                throw new RuntimeException("Insufficient balance");
            }
            account.setMoney(account.getMoney() + money);
        }
        accountDAO.save(account);
    }
}
