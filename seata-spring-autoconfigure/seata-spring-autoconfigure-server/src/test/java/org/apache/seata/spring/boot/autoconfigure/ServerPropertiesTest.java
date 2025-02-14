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
package org.apache.seata.spring.boot.autoconfigure;

import org.apache.seata.spring.boot.autoconfigure.properties.server.MetricsProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.ServerProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.ServerRecoveryProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.ServerUndoProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.store.DbcpProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.store.DruidProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.store.HikariProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.store.StoreDBProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.store.StoreFileProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.store.StoreProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.store.StoreRedisProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.store.StoreRedisProperties.Sentinel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;


public class ServerPropertiesTest {
    private static AnnotationConfigApplicationContext context;

    @BeforeAll
    public static void initContext() {
        context = new AnnotationConfigApplicationContext("org.apache.seata.spring.boot.autoconfigure.properties");
    }

    @Test
    public void testServerProperties() {
        assertFalse(context.getBean(ServerProperties.class).getRollbackRetryTimeoutUnlockEnable());
    }

    @Test
    public void testServerRecoveryProperties() {
        assertEquals(context.getBean(ServerRecoveryProperties.class).getAsyncCommittingRetryPeriod(), 1000);
        assertEquals(context.getBean(ServerRecoveryProperties.class).getCommittingRetryPeriod(), 1000);
        assertEquals(context.getBean(ServerRecoveryProperties.class).getRollbackingRetryPeriod(), 1000);
        assertEquals(context.getBean(ServerRecoveryProperties.class).getTimeoutRetryPeriod(), 1000);
    }

    @Test
    public void testServerUndoProperties() {
        assertEquals(context.getBean(ServerUndoProperties.class).getLogDeletePeriod(), 86400000);
    }

    @Test
    public void testMetricsProperties() {
        assertEquals(context.getBean(MetricsProperties.class).getExporterList(), "prometheus");
    }

    @Test
    public void testStoreProperties() {
        assertEquals(context.getBean(StoreProperties.class).getMode(), "file");
    }

    @Test
    public void testStoreFileProperties() {
        assertEquals(context.getBean(StoreFileProperties.class).getDir(), "sessionStore");
    }

    @Test
    public void testStoreDBProperties() {
        assertEquals(context.getBean(StoreDBProperties.class).getDatasource(), "druid");
    }

    @Test
    public void testStoreRedisProperties() {
        assertEquals(context.getBean(StoreRedisProperties.class).getMode(), "single");
    }

    @Test
    public void testStoreRedisPropertiesSingle() {
        assertEquals(context.getBean(StoreRedisProperties.Single.class).getHost(), "127.0.0.1");
    }

    @Test
    public void testStoreRedisPropertiesSentinel() {
        assertNull(context.getBean(Sentinel.class).getSentinelHosts());
    }

    @Test
    public void testHikariProperties() {
        HikariProperties hikariProperties = context.getBean(HikariProperties.class);
        Assertions.assertEquals(600000, hikariProperties.getIdleTimeout());
        Assertions.assertEquals(120000, hikariProperties.getKeepaliveTime());
        Assertions.assertEquals(1800000, hikariProperties.getMaxLifetime());
        Assertions.assertEquals(5000, hikariProperties.getValidationTimeout());
    }

    @Test
    public void testDruidProperties() {
        DruidProperties druidProperties = context.getBean(DruidProperties.class);

        Assertions.assertEquals(120000, druidProperties.getTimeBetweenEvictionRunsMillis());
        Assertions.assertEquals(300000, druidProperties.getMinEvictableIdleTimeMillis());
        Assertions.assertEquals(false, druidProperties.getKeepAlive());
        Assertions.assertEquals(false, druidProperties.getTestOnBorrow());
        Assertions.assertEquals(true, druidProperties.getTestWhileIdle());
    }

    @Test
    public void testDbcpProperties() {
        DbcpProperties dbcpProperties = context.getBean(DbcpProperties.class);
        Assertions.assertEquals(120000, dbcpProperties.getTimeBetweenEvictionRunsMillis());
        Assertions.assertEquals(300000, dbcpProperties.getMinEvictableIdleTimeMillis());
        Assertions.assertEquals(false, dbcpProperties.getTestOnBorrow());
        Assertions.assertEquals(true, dbcpProperties.getTestWhileIdle());
    }

    @AfterAll
    public static void closeContext() {
        context.close();
    }
}
