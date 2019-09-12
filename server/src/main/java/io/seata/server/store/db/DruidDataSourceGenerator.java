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
package io.seata.server.store.db;

import com.alibaba.druid.pool.DruidDataSource;
import io.seata.common.loader.LoadLevel;
import io.seata.core.store.db.AbstractDataSourceGenerator;

import javax.sql.DataSource;

/**
 * The type Druid data source generator.
 *
 * @author zhangsen
 * @data 2019 /4/28
 */
@LoadLevel(name = "druid")
public class DruidDataSourceGenerator extends AbstractDataSourceGenerator {

    @Override
    public DataSource generateDataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName(getDriverClassName());
        ds.setUrl(getUrl());
        ds.setUsername(getUser());
        ds.setPassword(getPassword());
        ds.setInitialSize(getMinConn());
        ds.setMaxActive(getMaxConn());
        ds.setMinIdle(getMinConn());
        ds.setMaxWait(5000);
        ds.setTimeBetweenEvictionRunsMillis(120000);
        ds.setMinEvictableIdleTimeMillis(300000);
        ds.setTestWhileIdle(true);
        ds.setTestOnBorrow(true);
        ds.setPoolPreparedStatements(true);
        ds.setMaxPoolPreparedStatementPerConnectionSize(20);
        ds.setValidationQuery(getValidationQuery(getDBType()));
        ds.setDefaultAutoCommit(true);
        return ds;
    }
}
