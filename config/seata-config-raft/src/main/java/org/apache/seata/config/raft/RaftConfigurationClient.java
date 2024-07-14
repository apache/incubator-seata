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
package org.apache.seata.config.raft;

import org.apache.seata.config.AbstractConfiguration;
import org.apache.seata.config.ConfigurationChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * The type Raft configuration of client.
 *
 */
public class RaftConfigurationClient extends AbstractConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaftConfigurationClient.class);
    private static volatile RaftConfigurationClient instance;

    public static RaftConfigurationClient getInstance() {
        if (instance == null) {
            synchronized (RaftConfigurationClient.class) {
                if (instance == null) {
                    instance = new RaftConfigurationClient();
                }
            }
        }
        return instance;
    }

    private RaftConfigurationClient() {
        initClientConfig();
    }

    private static void initClientConfig() {
        // acquire configs from server
        // 0.发送/cluster获取raft集群
        // 1.向raft集群发送getAll请求
        // 2.等待Raft日志提交，leader从rocksdb中读取全部配置返回(保证一致性)
        // 3.加载到seataConfig
        // 4.定期轮询配置变更
    }

    @Override
    public String getTypeName() {
        return null;
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        return false;
    }

    @Override
    public String getLatestConfig(String dataId, String defaultValue, long timeoutMills) {
        return null;
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        return false;
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        return false;
    }

    @Override
    public void addConfigListener(String dataId, ConfigurationChangeListener listener) {

    }

    @Override
    public void removeConfigListener(String dataId, ConfigurationChangeListener listener) {

    }

    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        return null;
    }

}
