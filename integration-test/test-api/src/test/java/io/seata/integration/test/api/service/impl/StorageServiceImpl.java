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
import io.seata.integration.test.api.service.StorageService;
import io.seata.integration.test.api.utils.DataSourceUtil;

/**
 * The type Storage service.
 */
public class StorageServiceImpl extends AbstractDataCheck implements StorageService {
    /**
     * The constant DB_KEY.
     */
    public static final String DB_KEY = "storage";

    @Override
    public void deduct(String commodityCode, int count) throws SQLException {
        String sql = "update storage_tbl set count = count - " + count + " where commodity_code = '" + commodityCode
                + "'";
        DataSourceUtil.executeUpdate(DB_KEY, sql);
    }

    @Override
    public void reset(String key, String value) throws SQLException {
        String deleteSql = "delete from storage_tbl where commodity_code = '" + key + "'";
        String insertSql = "insert into storage_tbl(commodity_code, count) values ('" + key + "', " + value + ")";
        DataSourceUtil.executeUpdate(DB_KEY, deleteSql);
        DataSourceUtil.executeUpdate(DB_KEY, insertSql);
    }

    @Override
    public int doNegativeCheck(String field, String id) throws SQLException {
        String checkSql = "select " + field + " from storage_tbl where commodity_code='" + id + "'";
        String result = DataSourceUtil.getSingleResult(DB_KEY, checkSql);
        return Integer.parseInt(result);
    }
}
