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

import org.apache.seata.common.pool.PoolManager;
import org.apache.seata.rm.datasource.SeataDataSourceProxy;

import java.sql.SQLException;

// Factory class to create appropriate PoolManager based on the type of DataSource.
public class PoolManagerFactory {

    private static final String HIKARI_CLASS_NAME = "com.zaxxer.hikari.HikariDataSource";
    private static final String DRUID_CLASS_NAME = "com.alibaba.druid.pool.DruidDataSource";

    public static PoolManager create(SeataDataSourceProxy proxy, String serviceName) {
        try {
            Class<?> hikariClass = Class.forName(HIKARI_CLASS_NAME);
            Object hikariDs = proxy.unwrap(hikariClass);
            if (hikariClass.isInstance(hikariDs)) {
                return new ReflectionHikariPoolManager(proxy, serviceName);
            }
        } catch (ClassNotFoundException ignored) {
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to unwrap HikariDataSource from proxy: " +
                    proxy.getTargetDataSource().getClass().getName(), e);
        }

        try {
            Class<?> druidClass = Class.forName(DRUID_CLASS_NAME);
            Object druidDs = proxy.unwrap(druidClass);
            if (druidClass.isInstance(druidDs)) {
                return new ReflectionDruidPoolManager(proxy, serviceName);
            }
        } catch (ClassNotFoundException ignored) {
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to unwrap DruidDataSource from proxy: " +
                    proxy.getTargetDataSource().getClass().getName(), e);
        }

        throw new IllegalArgumentException("No supported connection pool implementation found on proxy: " +
                proxy.getTargetDataSource().getClass().getName());
    }
}
