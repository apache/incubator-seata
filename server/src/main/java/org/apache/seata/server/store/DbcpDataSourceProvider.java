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

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.core.store.db.AbstractDataSourceProvider;
import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.apache.seata.common.DefaultValues.DEFAULT_DB_DBCP_MIN_EVICTABLE_TIME_MILLIS;
import static org.apache.seata.common.DefaultValues.DEFAULT_DB_DBCP_TEST_ON_BORROW;
import static org.apache.seata.common.DefaultValues.DEFAULT_DB_DBCP_TEST_WHILE_IDLE;
import static org.apache.seata.common.DefaultValues.DEFAULT_DB_DBCP_TIME_BETWEEN_EVICTION_RUNS_MILLIS;

/**
 * The dbcp datasource provider
 */
@LoadLevel(name = "dbcp")
public class DbcpDataSourceProvider extends AbstractDataSourceProvider {

    @Override
    public DataSource doGenerate() {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName(getDriverClassName());
        // DriverClassLoader works if upgrade commons-dbcp to at least 1.3.1.
        // https://issues.apache.org/jira/browse/DBCP-333
        ds.setDriverClassLoader(getDriverClassLoader());
        ds.setUrl(getUrl());
        ds.setUsername(getUser());

        ds.setPassword(getPassword());
        ds.setInitialSize(getMinConn());
        ds.setMaxTotal(getMaxConn());
        ds.setMinIdle(getMinConn());
        ds.setMaxIdle(getMinConn());
        ds.setMaxWaitMillis(getMaxWait());
        ds.setNumTestsPerEvictionRun(1);
        ds.setValidationQuery(getValidationQuery(getDBType()));
        ds.setConnectionProperties("useUnicode=yes;characterEncoding=utf8;socketTimeout=5000;connectTimeout=500");
        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

        long timeBetweenEvictionRunsMillis = CONFIG.getLong(ConfigurationKeys.STORE_DB_DBCP_TIME_BETWEEN_EVICTION_RUNS_MILLIS, DEFAULT_DB_DBCP_TIME_BETWEEN_EVICTION_RUNS_MILLIS);
        ds.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis < 0 ? DEFAULT_DB_DBCP_TIME_BETWEEN_EVICTION_RUNS_MILLIS : timeBetweenEvictionRunsMillis);
        long minEvictableIdleTimeMillis = CONFIG.getLong(ConfigurationKeys.STORE_DB_DBCP_MIN_EVICTABLE_TIME_MILLIS, DEFAULT_DB_DBCP_MIN_EVICTABLE_TIME_MILLIS);
        ds.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis < 0 ? DEFAULT_DB_DBCP_MIN_EVICTABLE_TIME_MILLIS : minEvictableIdleTimeMillis);
        boolean testWhileIdle = CONFIG.getBoolean(ConfigurationKeys.STORE_DB_DBCP_TEST_WHILE_IDLE, DEFAULT_DB_DBCP_TEST_WHILE_IDLE);
        ds.setTestWhileIdle(testWhileIdle);
        boolean testOnBorrow = CONFIG.getBoolean(ConfigurationKeys.STORE_DB_DBCP_TEST_ON_BORROW, DEFAULT_DB_DBCP_TEST_ON_BORROW);
        ds.setTestOnBorrow(testOnBorrow);
        return ds;
    }
}
