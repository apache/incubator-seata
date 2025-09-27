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

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.mcp.store.db.AbstractMCPDataSourceProvider;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * The dbcp datasource provider
 */
@LoadLevel(name = "dbcp")
public class DbcpDataSourceProvider extends AbstractMCPDataSourceProvider {

    @Override
    public DataSource doGenerate() {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName(getDriverClassName());
        ds.setDriverClassLoader(getDriverClassLoader());
        ds.setUrl(getUrl());
        ds.setUsername(getUser());
        ds.setPassword(getPassword());
        ds.setInitialSize(getMinConn());
        ds.setMaxTotal(getMaxConn());
        ds.setMinIdle(getMinConn());
        ds.setMaxIdle(getMinConn());
        ds.setMaxWaitMillis(getMaxWait());

        ds.setMaxConnLifetimeMillis(300000); // Maximum connection lifetime (5 minutes)
        ds.setLogExpiredConnections(true); // Log expired connections
        ds.setConnectionProperties(
                "useUnicode=yes;characterEncoding=utf8;socketTimeout=5000;connectTimeout=500;autoReconnect=true;maxReconnects=3;retriesAllDown=3");

        ds.setTestOnCreate(true); // Validate connection on creation
        ds.setTestOnBorrow(true); // Validate connection on borrow
        ds.setTestWhileIdle(true); // Validate idle connections
        ds.setValidationQuery(getValidationQuery(getDBType()));
        ds.setValidationQueryTimeout(5); // Validation query timeout (seconds)

        ds.setFastFailValidation(true); // Fast validation failure
        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

        ds.setMaxWaitMillis(5000); // Maximum wait time of 5 seconds
        ds.setAbandonedUsageTracking(true); // Track connection usage
        ds.setRemoveAbandonedOnBorrow(true); // Check for abandoned connections on borrow
        ds.setRemoveAbandonedOnMaintenance(true); // Remove abandoned connections during maintenance
        ds.setRemoveAbandonedTimeout(60); // Mark as abandoned after 60 seconds

        return ds;
    }
}
