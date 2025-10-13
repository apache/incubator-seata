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

import org.apache.seata.core.protocol.DruidConnectionPoolMetrics;
import org.apache.seata.core.protocol.HikariConnectionPoolMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * DataSource connection pool metrics collector for HikariCP and Druid.
 *
 * @since 2.1.0
 */
public class DataSourceConnectionPoolCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceConnectionPoolCollector.class);

    // Cache for registered DataSources
    private static final ConcurrentMap<String, DataSource> REGISTERED_DATASOURCES = new ConcurrentHashMap<>();

    // Supported DataSource (pool) types
    private static final String HIKARI_DATASOURCE_CLASS = "com.zaxxer.hikari.HikariDataSource";
    private static final String DRUID_DATASOURCE_CLASS = "com.alibaba.druid.pool.DruidDataSource";

    /**
     * Register a DataSource for monitoring
     */
    public static void registerDataSource(String name, DataSource dataSource) {
        if (name != null && dataSource != null) {
            REGISTERED_DATASOURCES.put(name, dataSource);
            LOGGER.info(
                    "Registered DataSource for monitoring: {} - {}",
                    name,
                    dataSource.getClass().getSimpleName());
        }
    }

    /**
     * Unregister a DataSource from monitoring
     */
    public static void unregisterDataSource(String name) {
        DataSource removed = REGISTERED_DATASOURCES.remove(name);
        if (removed != null) {
            LOGGER.info("Unregistered DataSource: {}", name);
        }
    }

    /**
     * Collect connection pool metrics for all registered DataSources
     */
    public static List<Object> collectAllPoolMetrics() {
        List<Object> allMetrics = new ArrayList<>();

        for (Map.Entry<String, DataSource> entry : REGISTERED_DATASOURCES.entrySet()) {
            String name = entry.getKey();
            DataSource dataSource = entry.getValue();

            try {
                Object metrics = collectPoolMetrics(name, dataSource);
                if (metrics != null) {
                    allMetrics.add(metrics);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to collect metrics for DataSource: {}", name, e);
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

            return metrics;

        } catch (Exception e) {
            LOGGER.error("Failed to collect Druid metrics for: {}", name, e);
            return null;
        }
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
     * Get the number of registered DataSources
     */
    public static int getRegisteredDataSourceCount() {
        return REGISTERED_DATASOURCES.size();
    }

    /**
     * Get names of all registered DataSources
     */
    public static List<String> getRegisteredDataSourceNames() {
        return new ArrayList<>(REGISTERED_DATASOURCES.keySet());
    }

    /**
     * Clear all registered DataSources
     */
    public static void clearAllDataSources() {
        REGISTERED_DATASOURCES.clear();
        LOGGER.info("Cleared all registered DataSources");
    }
}
