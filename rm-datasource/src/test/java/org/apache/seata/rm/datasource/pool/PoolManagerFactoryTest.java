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
import com.zaxxer.hikari.HikariDataSource;
import org.apache.seata.common.pool.PoolManager;
import org.apache.seata.rm.datasource.SeataDataSourceProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PoolManagerFactoryTest {

    private SeataDataSourceProxy proxy;

    @BeforeEach
    void setUp() {
        proxy = mock(SeataDataSourceProxy.class);
    }

    @Test
    void createReturnsHikariPoolManagerWhenProxyWrapsHikari() throws SQLException {
        HikariDataSource hikari = mock(HikariDataSource.class);
        when(proxy.unwrap(HikariDataSource.class)).thenReturn(hikari);

        PoolManager mgr = PoolManagerFactory.create(proxy, "svc-h");
        assertInstanceOf(ReflectionHikariPoolManager.class, mgr);

        verify(proxy, times(2)).unwrap(HikariDataSource.class);
        verify(proxy, never()).unwrap(DruidDataSource.class);
    }

    @Test
    void createReturnsDruidPoolManagerWhenProxyWrapsDruid() throws SQLException {
        DruidDataSource druid = new DruidDataSource();
        when(proxy.unwrap(HikariDataSource.class)).thenReturn(null);
        when(proxy.unwrap(DruidDataSource.class)).thenReturn(druid);

        PoolManager mgr = PoolManagerFactory.create(proxy, "svc-d");
        assertInstanceOf(ReflectionDruidPoolManager.class, mgr);

        verify(proxy, times(1)).unwrap(HikariDataSource.class);
        verify(proxy, times(2)).unwrap(DruidDataSource.class);
    }

    @Test
    void createThrowsWhenUnsupported() throws SQLException {
        when(proxy.unwrap(HikariDataSource.class)).thenReturn(null);
        when(proxy.unwrap(DruidDataSource.class)).thenReturn(null);
        DataSource unsupported = mock(DruidDataSource.class);
        when(proxy.getTargetDataSource()).thenReturn(unsupported);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> PoolManagerFactory.create(proxy, "svc-none"));
        String msg = ex.getMessage();
        assertTrue(msg.contains("No supported connection pool"));
        assertTrue(msg.contains(unsupported.getClass().getName()));
    }
}
