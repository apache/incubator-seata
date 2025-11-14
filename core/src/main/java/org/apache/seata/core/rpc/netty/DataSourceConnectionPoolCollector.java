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
package org.apache.seata.core.rpc.netty;

import org.apache.seata.core.model.PoolConfigUpdateRequest;
import org.apache.seata.core.protocol.DruidConnectionPoolMetrics;
import org.apache.seata.core.protocol.HikariConnectionPoolMetrics;
import org.apache.seata.core.protocol.SlowSqlEntry;
import org.apache.seata.core.protocol.SqlExecutionEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * DataSource connection pool metrics collector for HikariCP and Druid.
 *
 */
public class DataSourceConnectionPoolCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceConnectionPoolCollector.class);

    // Cache for registered DataSources split by pool implementation
    private static final ConcurrentMap<String, DataSource> REGISTERED_HIKARI_DATASOURCES = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, DataSource> REGISTERED_DRUID_DATASOURCES = new ConcurrentHashMap<>();

    // Supported DataSource (pool) types
    private static final String HIKARI_DATASOURCE_CLASS = "com.zaxxer.hikari.HikariDataSource";
    private static final String DRUID_DATASOURCE_CLASS = "com.alibaba.druid.pool.DruidDataSource";

    /**
     * Register a DataSource for monitoring
     */
    public static void registerDataSource(String name, DataSource dataSource) {
        if (name == null || dataSource == null) {
            return;
        }

        String className = dataSource.getClass().getName();
        if (HIKARI_DATASOURCE_CLASS.equals(className)) {
            REGISTERED_HIKARI_DATASOURCES.put(name, dataSource);
            LOGGER.info(
                    "Registered Hikari DataSource for monitoring: {} - {}",
                    name,
                    dataSource.getClass().getSimpleName());
        } else if (DRUID_DATASOURCE_CLASS.equals(className)) {
            REGISTERED_DRUID_DATASOURCES.put(name, dataSource);
            LOGGER.info(
                    "Registered Druid DataSource for monitoring: {} - {}",
                    name,
                    dataSource.getClass().getSimpleName());
        } else {
            LOGGER.debug("Unsupported DataSource type for registration: {} - {}", name, className);
        }
    }

    /**
     * Unregister a DataSource from monitoring
     */
    public static void unregisterDataSource(String name) {
        boolean removedFromHikari = REGISTERED_HIKARI_DATASOURCES.remove(name) != null;
        boolean removedFromDruid = REGISTERED_DRUID_DATASOURCES.remove(name) != null;

        if (removedFromHikari || removedFromDruid) {
            LOGGER.info("Unregistered DataSource: {}", name);
        } else {
            LOGGER.debug("No registered DataSource found to unregister: {}", name);
        }
    }

    /**
     * Collect connection pool metrics for all registered DataSources
     */
    public static List<HikariConnectionPoolMetrics> collectAllHikariPoolMetrics() {
        List<HikariConnectionPoolMetrics> allMetrics = new ArrayList<>();
        // Collect Hikari metrics
        for (ConcurrentMap.Entry<String, DataSource> entry : REGISTERED_HIKARI_DATASOURCES.entrySet()) {
            String name = entry.getKey();
            DataSource dataSource = entry.getValue();
            try {
                HikariConnectionPoolMetrics metrics = collectHikariMetrics(name, dataSource);
                if (metrics != null) {
                    LOGGER.info("RM Hikari Metrics :{}", metrics.getPoolName());
                    allMetrics.add(metrics);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to collect Hikari metrics for DataSource: {}", name, e);
            }
        }
        return allMetrics;
    }

    /**
     * Collect connection pool metrics for all registered DataSources
     */
    public static List<DruidConnectionPoolMetrics> collectAllDruidPoolMetrics() {
        List<DruidConnectionPoolMetrics> allMetrics = new ArrayList<>();
        // Collect Druid metrics
        for (ConcurrentMap.Entry<String, DataSource> entry : REGISTERED_DRUID_DATASOURCES.entrySet()) {
            String name = entry.getKey();
            DataSource dataSource = entry.getValue();
            try {
                DruidConnectionPoolMetrics metrics = collectDruidMetrics(name, dataSource);
                if (metrics != null) {
                    LOGGER.info("RM Druid Metrics :{}", metrics);
                    allMetrics.add(metrics);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to collect Druid metrics for DataSource: {}", name, e);
            }
        }
        return allMetrics;
    }

    /**
     * Collect connection pool metrics for a single DataSource
     */
    public static Object collectPoolMetrics(String name, DataSource dataSource) {
        if (dataSource == null) {
            return null;
        }

        String className = dataSource.getClass().getName();

        if (className.equals(HIKARI_DATASOURCE_CLASS)) {
            return collectHikariMetrics(name, dataSource);
        } else if (className.equals(DRUID_DATASOURCE_CLASS)) {
            return collectDruidMetrics(name, dataSource);
        } else {
            LOGGER.debug("Unsupported DataSource type: {}", className);
            return null;
        }
    }

    /**
     * Collect HikariCP connection pool metrics
     */
    private static HikariConnectionPoolMetrics collectHikariMetrics(String name, DataSource dataSource) {
        try {
            HikariConnectionPoolMetrics metrics = new HikariConnectionPoolMetrics(name);

            // Get HikariPoolMXBean
            Object poolMXBean = invokeMethod(dataSource, "getHikariPoolMXBean");
            if (poolMXBean == null) {
                LOGGER.warn("Failed to get HikariPoolMXBean for: {}", name);
                return null;
            }

            // Collect basic pool metrics
            metrics.setActiveConnections(getIntValue(poolMXBean, "getActiveConnections"));
            metrics.setIdleConnections(getIntValue(poolMXBean, "getIdleConnections"));
            metrics.setTotalConnections(getIntValue(poolMXBean, "getTotalConnections"));
            metrics.setWaitThreadCount(getIntValue(poolMXBean, "getThreadsAwaitingConnection"));

            // Collect configuration info
            metrics.setMaxPoolSize(getIntValue(dataSource, "getMaximumPoolSize"));
            metrics.setMinIdle(getIntValue(dataSource, "getMinimumIdle"));
            metrics.setConnectionTimeout(getIntValue(dataSource, "getConnectionTimeout"));
            metrics.setValidationTimeout(getLongValue(dataSource, "getValidationTimeout"));
            metrics.setAutoCommit(getBooleanValue(dataSource, "isAutoCommit"));
            metrics.setIdleTimeout(getLongValue(dataSource, "getIdleTimeout"));
            metrics.setLeakDetectionThreshold(getLongValue(dataSource, "getLeakDetectionThreshold"));
            metrics.setMaxLifeTime(getLongValue(dataSource, "getMaxLifetime"));
            metrics.setKeepaliveTime(getLongValue(dataSource, "getKeepaliveTime"));

            // Get JDBC URL and DataSource class name
            metrics.setJdbcUrl(getStringValue(dataSource, "getJdbcUrl"));
            metrics.setDataSourceClassName(getStringValue(dataSource, "getDataSourceClassName"));

            // Calculate connection acquisition time and timeout rate (via stats)
            calculateDerivedMetrics(metrics, poolMXBean);

            return metrics;

        } catch (Exception e) {
            LOGGER.error("Failed to collect HikariCP metrics for: {}", name, e);
            return null;
        }
    }

    /**
     * Collect Druid connection pool metrics
     */
    private static DruidConnectionPoolMetrics collectDruidMetrics(String name, DataSource dataSource) {
        try {
            DruidConnectionPoolMetrics metrics = new DruidConnectionPoolMetrics(name);

            // Collect basic pool metrics
            metrics.setActiveConnections(getIntValue(dataSource, "getActiveCount"));
            metrics.setIdleConnections(getIntValue(dataSource, "getPoolingCount"));
            metrics.setMaxPoolSize(getIntValue(dataSource, "getMaxActive"));
            metrics.setMinIdle(getIntValue(dataSource, "getMinIdle"));
            metrics.setWaitThreadCount(getIntValue(dataSource, "getWaitThreadCount"));

            // Compute total connections
            int activeCount = getIntValue(dataSource, "getActiveCount");
            int poolingCount = getIntValue(dataSource, "getPoolingCount");
            metrics.setTotalConnections(activeCount + poolingCount);

            // Collect configuration info
            metrics.setConnectionTimeout(getIntValue(dataSource, "getMaxWait"));
            metrics.setValidationTimeout(getLongValue(dataSource, "getValidationQueryTimeout"));
            metrics.setAutoCommit(getBooleanValue(dataSource, "isDefaultAutoCommit"));
            metrics.setIdleTimeout(getLongValue(dataSource, "getMinEvictableIdleTimeMillis"));
            metrics.setTimeBetweenEvictionRunsMills(getLongValue(dataSource, "getTimeBetweenEvictionRunsMillis"));
            metrics.setMaxEvictableTimeMills(getLongValue(dataSource, "getMaxEvictableIdleTimeMillis"));

            // Collect statistical info
            metrics.setExecuteCount(getLongValue(dataSource, "getExecuteCount"));
            metrics.setErrorCount(getLongValue(dataSource, "getErrorCount"));
            metrics.setCommitCount(getLongValue(dataSource, "getCommitCount"));
            metrics.setRollbackCount(getLongValue(dataSource, "getRollbackCount"));
            metrics.setLogicConnectCount(getLongValue(dataSource, "getConnectCount"));
            metrics.setPhysicalConnectCount(getLongValue(dataSource, "getCreateCount"));
            metrics.setPhysicalCloseCount(getLongValue(dataSource, "getDestroyCount"));

            // Get JDBC URL
            metrics.setJdbcUrl(getStringValue(dataSource, "getUrl"));
            metrics.setDataSourceClassName(dataSource.getClass().getName());

            // Get pool creation time
            Object createdTime = invokeMethod(dataSource, "getCreatedTime");
            if (createdTime instanceof java.util.Date) {
                metrics.setPoolCreatedTime(((java.util.Date) createdTime).getTime());
            }

            // Inject SQL execution records from SqlCollector
            try {
                List<SqlExecutionEntry> allSql = new ArrayList<>(SqlCollector.ALL_SQL_CACHE.asMap().values());
                metrics.setSqlExecutionRecord(limitEntries(allSql, 100));

                List<SqlExecutionEntry> slowSqlAsExec = new ArrayList<>();
                for (SlowSqlEntry s : SqlCollector.SLOW_SQL_CACHE.asMap().values()) {
                    slowSqlAsExec.add(
                            new SqlExecutionEntry(s.getSql(), s.getExecutionTimeMillis(), 0L, s.getTimestamp()));
                }
                metrics.setSlowSqlList(limitEntries(slowSqlAsExec, 100));

                // Compute transaction histogram based on execution times
                computeAndInjectHistogram(metrics, allSql);
            } catch (Throwable t) {
                LOGGER.debug("Failed to inject SQL records into Druid metrics", t);
            }

            return metrics;

        } catch (Exception e) {
            LOGGER.error("Failed to collect Druid metrics for: {}", name, e);
            return null;
        }
    }

    private static List<SqlExecutionEntry> limitEntries(List<SqlExecutionEntry> entries, int max) {
        if (entries == null) {
            return new ArrayList<>();
        }
        if (entries.size() <= max) {
            return entries;
        }
        return new ArrayList<>(entries.subList(0, max));
    }

    /**
     * Compute transaction time histogram and inject into metrics.
     * Ranges represent upper bounds (ms) for each bucket.
     */
    private static void computeAndInjectHistogram(DruidConnectionPoolMetrics metrics, List<SqlExecutionEntry> allSql) {
        long[] ranges = new long[]{10, 50, 100, 200, 500, 1000, 2000, 5000, 10000};
        long[] values = new long[ranges.length];
        if (allSql != null) {
            for (SqlExecutionEntry e : allSql) {
                long t = e.getExecutionTimeMillis();
                int idx = bucketIndexFor(t, ranges);
                if (idx >= 0) {
                    values[idx]++;
                }
            }
        }
        metrics.setTransactionHistogramRanges(ranges);
        metrics.setTransactionHistogramValues(values);
    }

    private static int bucketIndexFor(long t, long[] ranges) {
        for (int i = 0; i < ranges.length; i++) {
            if (t <= ranges[i]) {
                return i;
            }
        }
        return ranges.length - 1;
    }

    /**
     * Calculate derived metrics for HikariCP
     */
    private static void calculateDerivedMetrics(HikariConnectionPoolMetrics metrics, Object poolMXBean) {
        try {
            // More detailed stats can be fetched via JMX or other means
            // Temporarily use default values; real implementation may use historical monitoring data
            metrics.setConnectionAcquired(0L);
            metrics.setConnectionTimeoutRate(0.0);
        } catch (Exception e) {
            LOGGER.debug("Failed to calculate derived metrics", e);
        }
    }

    /**
     * Invoke method via reflection
     */
    private static Object invokeMethod(Object obj, String methodName) {
        try {
            Method method = obj.getClass().getMethod(methodName);
            return method.invoke(obj);
        } catch (Exception e) {
            LOGGER.debug(
                    "Failed to invoke method: {} on {}",
                    methodName,
                    obj.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * Get int value
     */
    private static int getIntValue(Object obj, String methodName) {
        Object value = invokeMethod(obj, methodName);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    /**
     * Get long value
     */
    private static long getLongValue(Object obj, String methodName) {
        Object value = invokeMethod(obj, methodName);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    /**
     * Get boolean value
     */
    private static boolean getBooleanValue(Object obj, String methodName) {
        Object value = invokeMethod(obj, methodName);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }

    /**
     * Get String value
     */
    private static String getStringValue(Object obj, String methodName) {
        Object value = invokeMethod(obj, methodName);
        return value != null ? value.toString() : null;
    }

    /**
     * Check whether the DataSource type is supported
     */
    public static boolean isSupportedDataSource(DataSource dataSource) {
        if (dataSource == null) {
            return false;
        }

        String className = dataSource.getClass().getName();
        return HIKARI_DATASOURCE_CLASS.equals(className) || DRUID_DATASOURCE_CLASS.equals(className);
    }

    /**
     * Update connection pool configuration for a specific DataSource.
     * This method uses reflection to dynamically update DataSource configuration parameters.
     */
    public static boolean updateConfig(String poolName, PoolConfigUpdateRequest request) {
        if (poolName == null || request == null) {
            LOGGER.warn("Cannot update pool config: poolName={}, request={}", poolName, request);
            return false;
        }

        // Thread-safe lookup in both Hikari and Druid registries
        DataSource dataSource = REGISTERED_HIKARI_DATASOURCES.get(poolName);
        if (dataSource != null) {
            return updateHikariConfig(poolName, dataSource, request);
        }

        dataSource = REGISTERED_DRUID_DATASOURCES.get(poolName);
        if (dataSource != null) {
            return updateDruidConfig(poolName, dataSource, request);
        }

        LOGGER.warn("No registered DataSource found for pool name: {}", poolName);
        return false;
    }

    /**
     * Update HikariCP DataSource configuration using reflection.
     */
    private static boolean updateHikariConfig(String poolName, DataSource dataSource, PoolConfigUpdateRequest request) {
        try {
            LOGGER.info("Updating HikariCP configuration for pool: {}", poolName);

            boolean success = true;

            // Update maximum pool size
            if (request.getMaxPoolSize() > 0) {
                success &= setIntValue(dataSource, "setMaximumPoolSize", request.getMaxPoolSize());
                LOGGER.debug("Updated maxPoolSize to {} for pool: {}", request.getMaxPoolSize(), poolName);
            }

            // Update minimum idle connections
            if (request.getMinIdle() >= 0) {
                success &= setIntValue(dataSource, "setMinimumIdle", request.getMinIdle());
                LOGGER.debug("Updated minIdle to {} for pool: {}", request.getMinIdle(), poolName);
            }

            // Update connection timeout
            if (request.getConnectionTimeout() > 0) {
                success &= setLongValue(dataSource, "setConnectionTimeout", (long) request.getConnectionTimeout());
                LOGGER.debug("Updated connectionTimeout to {} for pool: {}", request.getConnectionTimeout(), poolName);
            }

            // Update max lifetime (HikariCP specific)
            if (request.getMaxLifeTime() > 0) {
                success &= setLongValue(dataSource, "setMaxLifetime", request.getMaxLifeTime());
                LOGGER.debug("Updated maxLifetime to {} for pool: {}", request.getMaxLifeTime(), poolName);
            }

            // Update keepalive time (HikariCP specific)
            if (request.getKeepaliveTime() > 0) {
                success &= setLongValue(dataSource, "setKeepaliveTime", request.getKeepaliveTime());
                LOGGER.debug("Updated keepaliveTime to {} for pool: {}", request.getKeepaliveTime(), poolName);
            }

            if (success) {
                LOGGER.info("Successfully updated HikariCP configuration for pool: {}", poolName);
            } else {
                LOGGER.warn("Some configuration updates failed for HikariCP pool: {}", poolName);
            }

            return success;

        } catch (Exception e) {
            LOGGER.error("Failed to update HikariCP configuration for pool: {}", poolName, e);
            return false;
        }
    }

    /**
     * Update Druid DataSource configuration using reflection.
     */
    private static boolean updateDruidConfig(String poolName, DataSource dataSource, PoolConfigUpdateRequest request) {
        try {
            LOGGER.info("Updating Druid configuration for pool: {}", poolName);

            boolean success = true;

            // Update maximum pool size
            if (request.getMaxPoolSize() > 0) {
                success &= setIntValue(dataSource, "setMaxActive", request.getMaxPoolSize());
                LOGGER.debug("Updated maxActive to {} for pool: {}", request.getMaxPoolSize(), poolName);
            }

            // Update minimum idle connections
            if (request.getMinIdle() >= 0) {
                success &= setIntValue(dataSource, "setMinIdle", request.getMinIdle());
                LOGGER.debug("Updated minIdle to {} for pool: {}", request.getMinIdle(), poolName);
            }

            // Update connection timeout (mapped to maxWait in Druid)
            if (request.getConnectionTimeout() > 0) {
                success &= setLongValue(dataSource, "setMaxWait", (long) request.getConnectionTimeout());
                LOGGER.debug("Updated maxWait to {} for pool: {}", request.getConnectionTimeout(), poolName);
            }

            // Update time between eviction runs (Druid specific)
            if (request.getTimeBetweenEvictionRunsMills() > 0) {
                success &= setLongValue(
                        dataSource, "setTimeBetweenEvictionRunsMillis", request.getTimeBetweenEvictionRunsMills());
                LOGGER.debug(
                        "Updated timeBetweenEvictionRunsMillis to {} for pool: {}",
                        request.getTimeBetweenEvictionRunsMills(),
                        poolName);
            }

            // Update max evictable time (Druid specific)
            if (request.getMaxEvictableTimeMills() > 0) {
                success &=
                        setLongValue(dataSource, "setMinEvictableIdleTimeMillis", request.getMaxEvictableTimeMills());
                LOGGER.debug(
                        "Updated minEvictableIdleTimeMillis to {} for pool: {}",
                        request.getMaxEvictableTimeMills(),
                        poolName);
            }

            if (success) {
                LOGGER.info("Successfully updated Druid configuration for pool: {}", poolName);
            } else {
                LOGGER.warn("Some configuration updates failed for Druid pool: {}", poolName);
            }

            return success;

        } catch (Exception e) {
            LOGGER.error("Failed to update Druid configuration for pool: {}", poolName, e);
            return false;
        }
    }

    /**
     * Set int value using reflection with proper exception handling.
     */
    private static boolean setIntValue(Object obj, String methodName, int value) {
        try {
            Method method = obj.getClass().getMethod(methodName, int.class);
            method.invoke(obj, value);
            return true;
        } catch (Exception e) {
            LOGGER.debug(
                    "Failed to invoke method: {} with value: {} on {}",
                    methodName,
                    value,
                    obj.getClass().getSimpleName(),
                    e);
            return false;
        }
    }

    /**
     * Set long value using reflection with proper exception handling.
     */
    private static boolean setLongValue(Object obj, String methodName, long value) {
        try {
            Method method = obj.getClass().getMethod(methodName, long.class);
            method.invoke(obj, value);
            return true;
        } catch (Exception e) {
            LOGGER.debug(
                    "Failed to invoke method: {} with value: {} on {}",
                    methodName,
                    value,
                    obj.getClass().getSimpleName(),
                    e);
            return false;
        }
    }
}
