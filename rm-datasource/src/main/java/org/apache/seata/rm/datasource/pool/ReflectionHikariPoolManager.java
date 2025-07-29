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

import org.apache.seata.common.pool.ConnectionPoolConfig;
import org.apache.seata.common.pool.ConnectionPoolMetrics;
import org.apache.seata.common.pool.PoolManager;
import org.apache.seata.rm.datasource.SeataDataSourceProxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.time.Instant;

public class ReflectionHikariPoolManager implements PoolManager {

    private final Object dataSource; //actual HikariDataSource
    private final Object poolMXBean;
    private final String serviceName;

    public ReflectionHikariPoolManager(SeataDataSourceProxy proxy, String serviceName) {
        this.serviceName = serviceName;
        try {
            Class<?> dsClass = Class.forName("com.zaxxer.hikari.HikariDataSource");
            Method unwrap = proxy.getClass().getMethod("unwrap", Class.class);
            Object ds = unwrap.invoke(proxy, dsClass);
            if (ds == null) {
                throw new IllegalArgumentException(
                        "HikariPoolManager requires a HikariDataSource wrapped by Seata proxy");
            }
            this.dataSource = ds;
            Method getMx = dsClass.getMethod("getHikariPoolMXBean");
            this.poolMXBean = getMx.invoke(ds);

        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("HikariCP is not on the classpath", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SQLException) {
                throw new IllegalStateException("Failed to unwrap HikariDataSource", cause);
            }
            throw new IllegalStateException("Failed to initialize Hikari reflection", e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to initialize Hikari reflection", e);
        }
    }

    @Override
    public ConnectionPoolMetrics getMetric() {
        try {
            Class<?> mxClass = poolMXBean.getClass();
            int active = ((Number) mxClass
                    .getMethod("getActiveConnections")
                    .invoke(poolMXBean))
                    .intValue();

            int idle = ((Number) mxClass
                    .getMethod("getIdleConnections")
                    .invoke(poolMXBean))
                    .intValue();

            int total = ((Number) mxClass
                    .getMethod("getTotalConnections")
                    .invoke(poolMXBean))
                    .intValue();

            int waiting = ((Number) mxClass
                    .getMethod("getThreadsAwaitingConnection")
                    .invoke(poolMXBean))
                    .intValue();

            Number maxPoolN = (Number) dataSource.getClass()
                    .getMethod("getMaximumPoolSize")
                    .invoke(dataSource);
            int maxPool = maxPoolN.intValue();

            Number minIdleN = (Number) dataSource.getClass()
                    .getMethod("getMinimumIdle")
                    .invoke(dataSource);
            int minIdle = minIdleN.intValue();

            int connTimeout = ((Number) dataSource.getClass()
                    .getMethod("getConnectionTimeout")
                    .invoke(dataSource))
                    .intValue();

            long validationTimeout = ((Number) dataSource.getClass()
                    .getMethod("getValidationTimeout")
                    .invoke(dataSource))
                    .longValue();

            long idleTimeout = ((Number) dataSource.getClass()
                    .getMethod("getIdleTimeout")
                    .invoke(dataSource))
                    .longValue();

            boolean autoCommit = (Boolean) dataSource.getClass()
                    .getMethod("isAutoCommit")
                    .invoke(dataSource);

            long leakDetectionThreshold = ((Number) dataSource.getClass()
                    .getMethod("getLeakDetectionThreshold")
                    .invoke(dataSource))
                    .longValue();

            String poolName = (String) dataSource.getClass()
                    .getMethod("getPoolName")
                    .invoke(dataSource);

            return ConnectionPoolMetrics.builder()
                    .serviceName(serviceName)
                    .timestamp(Instant.now().toEpochMilli())
                    .poolName(poolName)
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
                    .leakDetectionThreshold(leakDetectionThreshold)
                    .build();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read Hikari metrics via reflection", e);
        }
    }

    @Override
    public ConnectionPoolConfig getConfig() {
        try {
            Class<?> dsClass = dataSource.getClass();

            int maxPool = ((Number) dsClass
                    .getMethod("getMaximumPoolSize")
                    .invoke(dataSource))
                    .intValue();

            int minIdle = ((Number) dsClass
                    .getMethod("getMinimumIdle")
                    .invoke(dataSource))
                    .intValue();

            int connectionTimeout = ((Number) dsClass
                    .getMethod("getConnectionTimeout")
                    .invoke(dataSource))
                    .intValue();

            long maxLifetime = ((Number) dsClass
                    .getMethod("getMaxLifetime")
                    .invoke(dataSource))
                    .longValue();

            Number keepaliveN;
            try {
                keepaliveN = (Number) dsClass
                        .getMethod("getKeepaliveTime")
                        .invoke(dataSource);
            } catch (NoSuchMethodException e) {
                keepaliveN = 0L;
            }
            long keepaliveTime = keepaliveN.longValue();


            return ConnectionPoolConfig.builder()
                    .maxPoolSize(maxPool)
                    .minIdle(minIdle)
                    .connectionTimeout(connectionTimeout)
                    .maxLifeTime(maxLifetime)
                    .keepaliveTime(keepaliveTime)
                    .build();

        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to reflectively read Hikari config", e);
        }
    }

    @Override
    public void updateConfig(ConnectionPoolConfig cfg) {
        try {
            Class<?> dsClass = dataSource.getClass();

            // maxPoolSize
            dsClass
                    .getMethod("setMaximumPoolSize", int.class)
                    .invoke(dataSource, cfg.getMaxPoolSize());

            // minIdle
            dsClass
                    .getMethod("setMinimumIdle", int.class)
                    .invoke(dataSource, cfg.getMinIdle());

            // connectionTimeout
            dsClass
                    .getMethod("setConnectionTimeout", long.class)
                    .invoke(dataSource, cfg.getConnectionTimeout());

            // maxLifetime
            dsClass
                    .getMethod("setMaxLifetime", long.class)
                    .invoke(dataSource, cfg.getMaxLifeTime());

            try {
                dsClass
                        .getMethod("setKeepaliveTime", long.class)
                        .invoke(dataSource, cfg.getKeepaliveTime());
            } catch (NoSuchMethodException ignored) {
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to reflectively update Hikari config", e);
        }
    }
}
