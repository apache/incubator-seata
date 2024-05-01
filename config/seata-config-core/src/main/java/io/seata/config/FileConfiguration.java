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
package io.seata.config;

import java.util.Set;

import org.apache.seata.common.exception.NotSupportYetException;

/**
 * The type File configuration.
 *
 * Notes: used for Apache ShardingSphere and ConfigurationFactory
 * 1.
 * https://github.com/apache/shardingsphere/blob/master/kernel/transaction/type/base/seata-at/src/main/java/org
 * /apache/shardingsphere/transaction/base/seata/at/SeataATShardingSphereTransactionManager.java
 * 2.EnhancedServiceLoader.load(ExtConfigurationProvider.class).provide(configuration)
 */
@Deprecated
public class FileConfiguration extends AbstractConfiguration {

    private final org.apache.seata.config.FileConfiguration target;

    /**
     * Instantiates a new File configuration.
     */
    public FileConfiguration() {
        target = new org.apache.seata.config.FileConfiguration();
    }

    /**
     * Instantiates a new File configuration.
     *
     * @param target the target
     */
    public FileConfiguration(org.apache.seata.config.Configuration target) {
        this.target = (org.apache.seata.config.FileConfiguration)target;
    }

    /**
     * Instantiates a new File configuration.
     *
     * @param name the name
     */
    public FileConfiguration(String name) {
        this(name, true);
    }

    /**
     * Instantiates a new File configuration.
     * For seata-server side the conf file should always exists.
     * For application(or client) side,conf file may not exists when using seata-spring-boot-starter
     *
     * @param name                the name
     * @param allowDynamicRefresh the allow dynamic refresh
     */
    public FileConfiguration(String name, boolean allowDynamicRefresh) {
        target = new org.apache.seata.config.FileConfiguration(name, allowDynamicRefresh);
    }

    @Override
    public String getTypeName() {
        return target.getTypeName();
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        return target.putConfig(dataId, content, timeoutMills);
    }

    @Override
    public String getLatestConfig(String dataId, String defaultValue, long timeoutMills) {
        return target.getLatestConfig(dataId, defaultValue, timeoutMills);
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        return target.putConfigIfAbsent(dataId, content, timeoutMills);
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        return target.removeConfig(dataId, timeoutMills);
    }

    @Override
    public void addConfigListener(String dataId, ConfigurationChangeListener listener) {
        throw new NotSupportYetException();
    }

    @Override
    public void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
        throw new NotSupportYetException();
    }

    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        return null;
    }
}
