/*
 *  Copyright 1999-2019 Seata.io Group.
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
package io.seata.integration.test.api.service.impl;

import java.sql.SQLException;

import io.seata.integration.test.api.service.AccountService;
import io.seata.integration.test.api.service.OrderService;
import io.seata.integration.test.api.utils.DataSourceUtil;

/**
 * The type Order service.
 *
 */
public class OrderServiceImpl implements OrderService {

    /**
     * The constant DB_KEY.
     */
    public static final String DB_KEY = "order";
    private AccountService accountService;

    @Override
    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public void create(String userId, String commodityCode, Integer count) throws SQLException {
        int money = count * 200;
        String sql = "insert into order_tbl (user_id, commodity_code, count, money) values ('" + userId + "','"
                + commodityCode + "'," + count + "," + money + ")";
        DataSourceUtil.executeUpdate(DB_KEY, sql);
        accountService.reduce(userId, money);

    }

    @Override
    public void reset(String key, String value) throws SQLException {
        String deleteSql = "delete from order_tbl";
        DataSourceUtil.executeUpdate(DB_KEY, deleteSql);
    }
}
