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

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.seata.common.monitor.SqlExecutionEntry;
import org.apache.seata.common.monitor.SqlMonitor;
import org.apache.seata.common.pool.ConnectionPoolConfig;
import org.apache.seata.common.pool.ConnectionPoolMetrics;
import org.apache.seata.rm.datasource.SeataDataSourceProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ReflectionDruidPoolManagerTest {

    private DruidDataSource ds;
    private SeataDataSourceProxy proxy;
    private ReflectionDruidPoolManager mgr;

    @BeforeEach
    public void setUp() throws SQLException {
        proxy = mock(SeataDataSourceProxy.class);
        ds = mock(DruidDataSource.class);
        when(proxy.unwrap(DruidDataSource.class)).thenReturn(ds);
        SqlMonitor.getInstance().resetForTest();
        SqlMonitor.getInstance().record("slow-sql", 2000, 5);
        SqlMonitor.getInstance().record("fast-sql", 100, 5);

        when(ds.getActiveCount()).thenReturn(5);
        when(ds.getPoolingCount()).thenReturn(3);
        when(ds.getMaxActive()).thenReturn(20);
        when(ds.getMinIdle()).thenReturn(2);
        when(ds.getNotEmptyWaitThreadCount()).thenReturn(1);
        when(ds.getMaxWait()).thenReturn(500L);
        when(ds.getValidationQueryTimeout()).thenReturn(2);
        when(ds.getMaxEvictableIdleTimeMillis()).thenReturn(1000L);
        when(ds.isDefaultAutoCommit()).thenReturn(false);
        when(ds.getExecuteCount()).thenReturn(10L);
        when(ds.getErrorCount()).thenReturn(1L);
        when(ds.getCommitCount()).thenReturn(2L);
        when(ds.getRollbackCount()).thenReturn(3L);
        when(ds.getConnectCount()).thenReturn(4L);
        when(ds.getTransactionHistogramValues()).thenReturn(new long[]{1, 2, 3});
        when(ds.getTransactionHistogramRanges()).thenReturn(new long[]{10, 20, 30});

        mgr = new ReflectionDruidPoolManager(proxy, "svc-A");
    }

    @Test
    public void testUnwrapNull() throws SQLException {
        when(proxy.unwrap(DruidDataSource.class)).thenReturn(null);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new ReflectionDruidPoolManager(proxy, "svc-A")
        );

        assertTrue(ex.getMessage().contains("requires a DruidDataSource"));
    }

    @Test
    public void testGetMetric() {
        ConnectionPoolMetrics m = mgr.getMetric();

        List<SqlExecutionEntry> slow = m.getSlowSqlList();
        assertEquals(1, slow.size());
        assertEquals("slow-sql", slow.get(0).getSql());

        List<SqlExecutionEntry> all = m.getSqlExecutionRecord();
        assertEquals(2, all.size());

        assertEquals(5, m.getActiveConnections());
        assertEquals(3, m.getIdleConnections());
        assertEquals(8, m.getTotalConnections());
        assertEquals(20, m.getMaxPoolSize());
        assertEquals(2, m.getMinIdle());
        assertEquals(1, m.getWaitThreadCount());
        assertEquals(500, m.getConnectionTimeout());
        assertEquals(2 * 1000, m.getValidationTimeout());
        assertEquals(1000L, m.getIdleTimeout());
        assertFalse(m.isAutoCommit());

        assertEquals(10L, m.getExecuteCount());
        assertEquals(1L, m.getErrorCount());
        assertEquals(2L, m.getCommitCount());
        assertEquals(3L, m.getRollbackCount());
        assertEquals(4L, m.getLogicConnectCount());

        assertArrayEquals(new long[]{1, 2, 3}, m.getTransactionHistogramValues());
        assertArrayEquals(new long[]{10, 20, 30}, m.getTransactionHistogramRanges());
        assertTrue(m.getTimestamp() > 0L);
    }

    @Test
    public void testGetConfig() {
        when(ds.getConnectTimeout()).thenReturn(700);
        when(ds.getTimeBetweenEvictionRunsMillis()).thenReturn(800L);
        when(ds.getMaxEvictableIdleTimeMillis()).thenReturn(900L);
        when(ds.getMaxActive()).thenReturn(20);
        when(ds.getMinIdle()).thenReturn(2);


        ConnectionPoolConfig cfg = mgr.getConfig();
        assertEquals("svc-A", cfg.getServiceName());
        assertEquals(20, cfg.getMaxPoolSize());
        assertEquals(2, cfg.getMinIdle());
        assertEquals(700, cfg.getConnectionTimeout());
        assertEquals(800L, cfg.getTimeBetweenEvictionRunsMills());
        assertEquals(900L, cfg.getMaxEvictableTimeMills());
    }

    @Test
    public void testUpdateConfig() {
        ConnectionPoolConfig newCfg = ConnectionPoolConfig.builder()
                .serviceName("svc-A")
                .maxPoolSize(30)
                .minIdle(3)
                .connectionTimeout(400)
                .timeBetweenEvictionRunsMills(500L)
                .maxEvictableTimeMills(600L)
                .build();

        mgr.updateConfig(newCfg);

        verify(ds).setMaxActive(30);
        verify(ds).setMinIdle(3);
        verify(ds).setConnectTimeout(400);
        verify(ds).setTimeBetweenEvictionRunsMillis(500L);
        verify(ds).setMaxEvictableIdleTimeMillis(600L);
    }
}
