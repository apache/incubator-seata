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
package org.apache.seata.rm.datasource.pool;


import org.apache.seata.common.monitor.SqlExecutionEntry;
import org.apache.seata.common.monitor.SqlMonitor;
import org.apache.seata.common.pool.ConnectionPoolConfig;
import org.apache.seata.common.pool.ConnectionPoolMetrics;
import org.apache.seata.common.pool.PoolManager;
import org.apache.seata.rm.datasource.SeataDataSourceProxy;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;

public class ReflectionDruidPoolManager implements PoolManager {

    private final Object dataSource;  // actual DruidDataSource
    private final String serviceName;

    public ReflectionDruidPoolManager(SeataDataSourceProxy proxy, String serviceName) {
        this.serviceName = serviceName;
        try {
            Class<?> dsClass = Class.forName("com.alibaba.druid.pool.DruidDataSource");
            Method unwrap = proxy.getClass().getMethod("unwrap", Class.class);
            Object ds = unwrap.invoke(proxy, dsClass);
            if (ds == null) {
                throw new IllegalArgumentException(
                        "DruidPoolManager requires a DruidDataSource wrapped by Seata proxy");
            }
            this.dataSource = ds;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Druid is not on the classpath", e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to initialize Druid reflection", e);
        }
    }

    @Override
    public ConnectionPoolMetrics getMetric() {
        try {
            Class<?> dsClass = dataSource.getClass();

            // Collect slow SQL entries and all SQL entries
            List<SqlExecutionEntry> slowSqlList = SqlMonitor.getInstance().getSlowSqlList();
            List<SqlExecutionEntry> allSqlRecords = SqlMonitor.getInstance().getAllRecords();

            int active = ((Number) dsClass.getMethod("getActiveCount").invoke(dataSource)).intValue();
            int idle = ((Number) dsClass.getMethod("getPoolingCount").invoke(dataSource)).intValue();
            int total = active + idle;
            int maxPool = ((Number) dsClass.getMethod("getMaxActive").invoke(dataSource)).intValue();
            int minIdle = ((Number) dsClass.getMethod("getMinIdle").invoke(dataSource)).intValue();
            int waiting = ((Number) dsClass.getMethod("getNotEmptyWaitThreadCount").invoke(dataSource)).intValue();
            int connTimeout = ((Number) dsClass.getMethod("getMaxWait").invoke(dataSource)).intValue();
            long validationQueryT = ((Number) dsClass.getMethod("getValidationQueryTimeout").invoke(dataSource)).longValue();
            long validationTimeout = validationQueryT * 1000L;
            long idleTimeout = ((Number) dsClass.getMethod("getMaxEvictableIdleTimeMillis").invoke(dataSource)).longValue();
            boolean autoCommit = (boolean) dsClass.getMethod("isDefaultAutoCommit").invoke(dataSource);
            int executeCount = ((Number) dsClass.getMethod("getExecuteCount").invoke(dataSource)).intValue();
            int errorCount = ((Number) dsClass.getMethod("getErrorCount").invoke(dataSource)).intValue();
            int commitCount = ((Number) dsClass.getMethod("getCommitCount").invoke(dataSource)).intValue();
            int rollbackCount = ((Number) dsClass.getMethod("getRollbackCount").invoke(dataSource)).intValue();
            int logicConnectCount = ((Number) dsClass.getMethod("getConnectCount").invoke(dataSource)).intValue();
            long[] transactionHistogramValues = (long[]) dsClass.getMethod("getTransactionHistogramValues").invoke(dataSource);
            long[] transactionHistogramRanges = (long[]) dsClass.getMethod("getTransactionHistogramRanges").invoke(dataSource);


            return ConnectionPoolMetrics.builder()
                    .serviceName(serviceName)
                    .timestamp(Instant.now().toEpochMilli())
                    .activeConnections(active)
                    .idleConnections(idle)
                    .totalConnections(total)
                    .maxPoolSize(maxPool)
                    .minIdle(minIdle)
                    .waitThreadCount(waiting)
                    .connectionTimeout(connTimeout)
                    .validationTimeout(validationTimeout)
                    .idleTimeout(idleTimeout)
                    .autoCommit(autoCommit)
                    .executeCount(executeCount)
                    .errorCount(errorCount)
                    .commitCount(commitCount)
                    .rollbackCount(rollbackCount)
                    .logicConnectCount(logicConnectCount)
                    .transactionHistogramValues(transactionHistogramValues)
                    .transactionHistogramRanges(transactionHistogramRanges)
                    .slowSqlList(slowSqlList)
                    .sqlExecutionRecords(allSqlRecords)
                    .build();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read Druid metrics via reflection", e);
        }
    }

    @Override
    public ConnectionPoolConfig getConfig() {
        try {
            Class<?> dsClass = dataSource.getClass();

            int maxPool = ((Number) dsClass.getMethod("getMaxActive").invoke(dataSource)).intValue();
            int minIdle = ((Number) dsClass.getMethod("getMinIdle").invoke(dataSource)).intValue();
            int connTimeout = ((Number) dsClass.getMethod("getConnectTimeout")
                    .invoke(dataSource)).intValue();
            long evictionRun = ((Number) dsClass.getMethod("getTimeBetweenEvictionRunsMillis")
                    .invoke(dataSource)).longValue();
            long maxEvictable = ((Number) dsClass.getMethod("getMaxEvictableIdleTimeMillis")
                    .invoke(dataSource)).longValue();

            return ConnectionPoolConfig.builder()
                    .serviceName(serviceName)
                    .maxPoolSize(maxPool)
                    .minIdle(minIdle)
                    .connectionTimeout(connTimeout)
                    .timeBetweenEvictionRunsMills(evictionRun)
                    .maxEvictableTimeMills(maxEvictable)
                    .build();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read Druid config via reflection", e);
        }
    }

    @Override
    public void updateConfig(ConnectionPoolConfig cfg) {
        try {
            Class<?> dsClass = dataSource.getClass();
            dsClass.getMethod("setMaxActive", int.class)
                    .invoke(dataSource, cfg.getMaxPoolSize());
            dsClass.getMethod("setMinIdle", int.class)
                    .invoke(dataSource, cfg.getMinIdle());
            dsClass.getMethod("setConnectTimeout", int.class)
                    .invoke(dataSource, cfg.getConnectionTimeout());
            dsClass.getMethod("setTimeBetweenEvictionRunsMillis", long.class)
                    .invoke(dataSource, cfg.getTimeBetweenEvictionRunsMills());
            dsClass.getMethod("setMaxEvictableIdleTimeMillis", long.class)
                    .invoke(dataSource, cfg.getMaxEvictableTimeMills());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to update Druid config via reflection", e);
        }
    }
}
