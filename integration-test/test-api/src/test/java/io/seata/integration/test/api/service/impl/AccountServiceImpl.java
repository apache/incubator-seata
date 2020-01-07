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

import io.seata.integration.test.api.service.AbstractDataCheck;
import io.seata.integration.test.api.service.AccountService;
import io.seata.integration.test.api.utils.DataSourceUtil;

/**
 * The type Account service.
 */
public class AccountServiceImpl extends AbstractDataCheck implements AccountService {

    /**
     * The constant DB_KEY.
     */
    public static final String DB_KEY = "account";

    @Override
    public void reduce(String userId, int money) throws SQLException {
        String sql = "update account_tbl set money = money - " + money + " where user_id = '" + userId + "'";
        DataSourceUtil.executeUpdate(DB_KEY, sql);
    }

    @Override
    public int doNegativeCheck(String field, String id) throws SQLException {
        String checkSql = "select " + field + " from account_tbl where user_id='" + id + "'";
        String result = DataSourceUtil.getSingleResult(DB_KEY, checkSql);
        return Integer.parseInt(result);
    }

    @Override
    public void reset(String key, String value) throws SQLException {
        String deleteSql = "delete from account_tbl where user_id = '" + key + "'";
        String insertSql = "insert into account_tbl(user_id, money) values ('" + key + "', " + value + ")";
        DataSourceUtil.executeUpdate(DB_KEY, deleteSql);
        DataSourceUtil.executeUpdate(DB_KEY, insertSql);
    }
}
