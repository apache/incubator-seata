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


import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.apache.seata.common.pool.ConnectionPoolConfig;
import org.apache.seata.common.pool.ConnectionPoolMetrics;
import org.apache.seata.rm.datasource.SeataDataSourceProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.SQLException;

import static org.mockito.Mockito.*;

/**
 * Test for ReflectionHikariPoolManager
 */
public class ReflectionHikariPoolManagerTest {

    private HikariDataSource ds;
    private HikariPoolMXBean mxBean;
    private SeataDataSourceProxy proxy;
    private ReflectionHikariPoolManager mgr;

    @BeforeEach
    public void setUp() throws SQLException {
        proxy = mock(SeataDataSourceProxy.class);
        ds = mock(HikariDataSource.class);
        when(proxy.unwrap(HikariDataSource.class)).thenReturn(ds);

        mxBean = mock(HikariPoolMXBean.class);
        when(ds.getHikariPoolMXBean()).thenReturn(mxBean);
        when(mxBean.getActiveConnections()).thenReturn(2);
        when(mxBean.getIdleConnections()).thenReturn(3);
        when(mxBean.getTotalConnections()).thenReturn(5);
        when(mxBean.getThreadsAwaitingConnection()).thenReturn(1);
        when(ds.getMaximumPoolSize()).thenReturn(10);
        when(ds.getMinimumIdle()).thenReturn(1);
        when(ds.getConnectionTimeout()).thenReturn(1000L);
        when(ds.getValidationTimeout()).thenReturn(2000L);
        when(ds.getIdleTimeout()).thenReturn(3000L);
        when(ds.isAutoCommit()).thenReturn(true);
        when(ds.getLeakDetectionThreshold()).thenReturn(4000L);
        when(ds.getPoolName()).thenReturn("pool1");

        mgr = new ReflectionHikariPoolManager(proxy, "svcName");
    }

    @Test
    public void testWhenUnwrapReturnNull() throws SQLException {
        when(proxy.unwrap(HikariDataSource.class)).thenReturn(null);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new ReflectionHikariPoolManager(proxy, "svc")
        );
        assertTrue(ex.getMessage().contains("requires a HikariDataSource"));
    }


    @Test
    public void testGetMetrics() {
        ConnectionPoolMetrics m = mgr.getMetric();

        assertEquals("svcName", m.getServiceName());
        assertEquals("pool1", m.getPoolName());
        assertEquals(2, m.getActiveConnections());
        assertEquals(3, m.getIdleConnections());
        assertEquals(5, m.getTotalConnections());
        assertEquals(1, m.getWaitThreadCount());
        assertEquals(10, m.getMaxPoolSize());
        assertEquals(1, m.getMinIdle());
        assertEquals(1000L, m.getConnectionTimeout());
        assertEquals(2000L, m.getValidationTimeout());
        assertEquals(3000L, m.getIdleTimeout());
        assertTrue(m.isAutoCommit());
        assertEquals(4000L, m.getLeakDetectionThreshold());
        assertTrue(m.getTimestamp() > 0L, "timestamp should be set");
    }

    @Test
    public void testGetConfig() {
        when(ds.getMaxLifetime()).thenReturn(5000L);
        when(ds.getKeepaliveTime()).thenReturn(6000L);

        ConnectionPoolConfig cfg = mgr.getConfig();
        assertEquals(10, cfg.getMaxPoolSize());
        assertEquals(1, cfg.getMinIdle());
        assertEquals(1000L, cfg.getConnectionTimeout());
        assertEquals(5000L, cfg.getMaxLifeTime());
        assertEquals(6000L, cfg.getKeepaliveTime());
    }

    @Test
    public void testUpdateConfig() {
        // Create new config
        ConnectionPoolConfig newConfig = ConnectionPoolConfig.builder()
                .maxPoolSize(20)
                .minIdle(5)
                .connectionTimeout(1100)
                .maxLifeTime(5200L)
                .keepaliveTime(6200L)
                .build();

        mgr.updateConfig(newConfig);

        // Verify changes
        verify(ds).setMaximumPoolSize(20);
        verify(ds).setMinimumIdle(5);
        verify(ds).setConnectionTimeout(1100L);
        verify(ds).setMaxLifetime(5200L);
        verify(ds).setKeepaliveTime(6200L);
    }

}