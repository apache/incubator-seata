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
package org.apache.seata.server.store;

import javax.sql.DataSource;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.core.store.db.AbstractDataSourceProvider;
import com.alibaba.druid.pool.DruidDataSource;

import java.sql.Connection;

import static org.apache.seata.common.DefaultValues.DEFAULT_DB_DRUID_KEEP_ALIVE;
import static org.apache.seata.common.DefaultValues.DEFAULT_DB_DRUID_MIN_EVICTABLE_TIME_MILLIS;
import static org.apache.seata.common.DefaultValues.DEFAULT_DB_DRUID_TEST_ON_BORROW;
import static org.apache.seata.common.DefaultValues.DEFAULT_DB_DRUID_TEST_WHILE_IDLE;
import static org.apache.seata.common.DefaultValues.DEFAULT_DB_DRUID_TIME_BETWEEN_EVICTION_RUNS_MILLIS;

/**
 * The druid datasource provider
 */
@LoadLevel(name = "druid")
public class DruidDataSourceProvider extends AbstractDataSourceProvider {

    @Override
    public DataSource doGenerate() {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName(getDriverClassName());
        ds.setDriverClassLoader(getDriverClassLoader());
        ds.setUrl(getUrl());
        ds.setUsername(getUser());
        ds.setPassword(getPassword());
        ds.setInitialSize(getMinConn());
        ds.setMaxActive(getMaxConn());
        ds.setMinIdle(getMinConn());
        ds.setMaxWait(getMaxWait());

        long timeBetweenEvictionRunsMillis = CONFIG.getLong(ConfigurationKeys.STORE_DB_DRUID_TIME_BETWEEN_EVICTION_RUNS_MILLIS, DEFAULT_DB_DRUID_TIME_BETWEEN_EVICTION_RUNS_MILLIS);
        ds.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis < 0 ? DEFAULT_DB_DRUID_TIME_BETWEEN_EVICTION_RUNS_MILLIS : timeBetweenEvictionRunsMillis);
        long minEvictableIdleTimeMillis = CONFIG.getLong(ConfigurationKeys.STORE_DB_DRUID_MIN_EVICTABLE_TIME_MILLIS, DEFAULT_DB_DRUID_MIN_EVICTABLE_TIME_MILLIS);
        ds.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis < 0 ? DEFAULT_DB_DRUID_MIN_EVICTABLE_TIME_MILLIS : minEvictableIdleTimeMillis);
        boolean testWhileIdle = CONFIG.getBoolean(ConfigurationKeys.STORE_DB_DRUID_TEST_WHILE_IDLE, DEFAULT_DB_DRUID_TEST_WHILE_IDLE);
        ds.setTestWhileIdle(testWhileIdle);
        boolean testOnBorrow = CONFIG.getBoolean(ConfigurationKeys.STORE_DB_DRUID_TEST_ON_BORROW, DEFAULT_DB_DRUID_TEST_ON_BORROW);
        ds.setTestOnBorrow(testOnBorrow);
        boolean keepAlive = CONFIG.getBoolean(ConfigurationKeys.STORE_DB_DRUID_KEEP_ALIVE, DEFAULT_DB_DRUID_KEEP_ALIVE);
        ds.setKeepAlive(keepAlive);

        ds.setPoolPreparedStatements(true);
        ds.setMaxPoolPreparedStatementPerConnectionSize(20);
        ds.setValidationQuery(getValidationQuery(getDBType()));
        ds.setDefaultAutoCommit(true);
        // fix issue 5030
        ds.setUseOracleImplicitCache(false);
        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        return ds;
    }
}
