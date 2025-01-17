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

import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.seata.spring.boot.autoconfigure.properties.server.raft.ServerRaftSSLClientProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.raft.ServerRaftSSLProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.raft.ServerRaftSSLServerProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.store.StoreProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.MetricsProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.ServerProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.raft.ServerRaftProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.ServerRecoveryProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.ServerUndoProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.session.SessionProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.store.StoreDBProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.store.StoreFileProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.store.StoreRedisProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.store.StoreProperties.Lock;
import org.apache.seata.spring.boot.autoconfigure.properties.server.store.StoreProperties.Session;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.METRICS_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.PROPERTY_BEAN_MAP;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SERVER_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SERVER_RAFT_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SERVER_RAFT_SSL_CLIENT_KEYSTORE_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SERVER_RAFT_SSL_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SERVER_RAFT_SSL_SERVER_KEYSTORE_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SERVER_RECOVERY_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SERVER_UNDO_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SESSION_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.STORE_DB_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.STORE_FILE_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.STORE_LOCK_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.STORE_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.STORE_REDIS_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.STORE_REDIS_SENTINEL_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.STORE_REDIS_SINGLE_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.STORE_SESSION_PREFIX;


public class SeataServerEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final AtomicBoolean INIT = new AtomicBoolean(false);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        init();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    public static void init() {
        if (INIT.compareAndSet(false, true)) {
            PROPERTY_BEAN_MAP.put(SERVER_PREFIX, ServerProperties.class);
            PROPERTY_BEAN_MAP.put(SERVER_UNDO_PREFIX, ServerUndoProperties.class);
            PROPERTY_BEAN_MAP.put(SERVER_RECOVERY_PREFIX, ServerRecoveryProperties.class);
            PROPERTY_BEAN_MAP.put(METRICS_PREFIX, MetricsProperties.class);
            PROPERTY_BEAN_MAP.put(STORE_SESSION_PREFIX, Session.class);
            PROPERTY_BEAN_MAP.put(STORE_LOCK_PREFIX, Lock.class);
            PROPERTY_BEAN_MAP.put(STORE_FILE_PREFIX, StoreFileProperties.class);
            PROPERTY_BEAN_MAP.put(STORE_DB_PREFIX, StoreDBProperties.class);
            PROPERTY_BEAN_MAP.put(STORE_REDIS_PREFIX, StoreRedisProperties.class);
            PROPERTY_BEAN_MAP.put(STORE_REDIS_SINGLE_PREFIX, StoreRedisProperties.Single.class);
            PROPERTY_BEAN_MAP.put(STORE_REDIS_SENTINEL_PREFIX, StoreRedisProperties.Sentinel.class);
            PROPERTY_BEAN_MAP.put(SERVER_RAFT_PREFIX, ServerRaftProperties.class);
            PROPERTY_BEAN_MAP.put(SERVER_RAFT_SSL_SERVER_KEYSTORE_PREFIX, ServerRaftSSLServerProperties.class);
            PROPERTY_BEAN_MAP.put(SERVER_RAFT_SSL_PREFIX, ServerRaftSSLProperties.class);
            PROPERTY_BEAN_MAP.put(SERVER_RAFT_SSL_CLIENT_KEYSTORE_PREFIX, ServerRaftSSLClientProperties.class);
            PROPERTY_BEAN_MAP.put(SESSION_PREFIX, SessionProperties.class);
            PROPERTY_BEAN_MAP.put(STORE_PREFIX, StoreProperties.class);
        }
    }

}
