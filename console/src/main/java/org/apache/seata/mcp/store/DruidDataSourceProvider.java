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
package org.apache.seata.mcp.store;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.mcp.store.db.AbstractMCPDataSourceProvider;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * The druid datasource provider
 */
@LoadLevel(name = "druid")
public class DruidDataSourceProvider extends AbstractMCPDataSourceProvider {

    @Override
    public DataSource doGenerate() {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName(getDriverClassName());
        ds.setDriverClassLoader(getDriverClassLoader());
        ds.setUrl(getUrl());
        ds.setUsername(getUser());
        ds.setPassword(getPassword());

        ds.setInitialSize(getMinConn()); // Initial connection pool size
        ds.setMaxActive(getMaxConn()); // Maximum active connections
        ds.setMinIdle(getMinConn()); // Minimum idle connections
        ds.setMaxWait(getMaxWait()); // Maximum wait time for connection acquisition

        ds.setTestOnBorrow(true); // Test connections when borrowing
        ds.setTestOnReturn(false); // Don't test on return for performance
        ds.setTestWhileIdle(true); // Test idle connections
        ds.setTimeBetweenEvictionRunsMillis(60000); // Run evictor every 60 seconds
        ds.setMinEvictableIdleTimeMillis(180000); // Min idle time before eviction (3 minutes)
        ds.setMaxEvictableIdleTimeMillis(300000); // Max idle time before eviction (5 minutes)
        ds.setValidationQuery(getValidationQuery(getDBType()));
        ds.setValidationQueryTimeout(5); // Validation timeout (seconds)

        ds.setConnectionErrorRetryAttempts(3); // Retry 3 times on connection error
        ds.setBreakAfterAcquireFailure(true); // Break after all retries fail

        ds.setPoolPreparedStatements(true); // Pool prepared statements for better performance
        ds.setMaxPoolPreparedStatementPerConnectionSize(20);
        ds.setDefaultAutoCommit(true);

        ds.setUseOracleImplicitCache(false);
        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

        return ds;
    }
}
