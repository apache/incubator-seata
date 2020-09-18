/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.spring.boot.autoconfigure;

import java.util.HashMap;

import io.seata.spring.boot.autoconfigure.properties.client.LockProperties;
import io.seata.spring.boot.autoconfigure.properties.client.LogProperties;
import io.seata.spring.boot.autoconfigure.properties.client.RmProperties;
import io.seata.spring.boot.autoconfigure.properties.client.ServiceProperties;
import io.seata.spring.boot.autoconfigure.properties.client.ShutdownProperties;
import io.seata.spring.boot.autoconfigure.properties.client.ThreadFactoryProperties;
import io.seata.spring.boot.autoconfigure.properties.client.TmProperties;
import io.seata.spring.boot.autoconfigure.properties.client.TransportProperties;
import io.seata.spring.boot.autoconfigure.properties.client.UndoProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigApolloProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigConsulProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigEtcd3Properties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigFileProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigNacosProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigZooKeeperProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryConsulProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryEtcd3Properties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryEurekaProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryNacosProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryRedisProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistrySofaProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryZooKeeperProperties;

/**
 * @author xingfudeshi@gmail.com
 */
public interface StarterConstants {
    int MAP_CAPACITY = 64;
    String SEATA_PREFIX = "seata";
    String SEATA_SPRING_CLOUD_ALIBABA_PREFIX = "spring.cloud.alibaba.seata";
    String TRANSPORT_PREFIX = SEATA_PREFIX + ".transport";
    String THREAD_FACTORY_PREFIX_KEBAB_STYLE = TRANSPORT_PREFIX + ".thread-factory";
    String THREAD_FACTORY_PREFIX = TRANSPORT_PREFIX + ".threadFactory";
    String SHUTDOWN_PREFIX = TRANSPORT_PREFIX + ".shutdown";
    String SERVICE_PREFIX = SEATA_PREFIX + ".service";
    String CLIENT_PREFIX = SEATA_PREFIX + ".client";
    String CLIENT_RM_PREFIX = CLIENT_PREFIX + ".rm";
    String CLIENT_TM_PREFIX = CLIENT_PREFIX + ".tm";
    String LOCK_PREFIX = CLIENT_RM_PREFIX + ".lock";
    String UNDO_PREFIX = CLIENT_PREFIX + ".undo";
    String LOG_PREFIX = CLIENT_PREFIX + ".log";

    String REGISTRY_PREFIX = SEATA_PREFIX + ".registry";
    String REGISTRY_NACOS_PREFIX = REGISTRY_PREFIX + ".nacos";
    String REGISTRY_EUREKA_PREFIX = REGISTRY_PREFIX + ".eureka";
    String REGISTRY_REDIS_PREFIX = REGISTRY_PREFIX + ".redis";
    String REGISTRY_ZK_PREFIX = REGISTRY_PREFIX + ".zk";
    String REGISTRY_CONSUL_PREFIX = REGISTRY_PREFIX + ".consul";
    String REGISTRY_ETCD3_PREFIX = REGISTRY_PREFIX + ".etcd3";
    String REGISTRY_SOFA_PREFIX = REGISTRY_PREFIX + ".sofa";

    String CONFIG_PREFIX = SEATA_PREFIX + ".config";
    String CONFIG_NACOS_PREFIX = CONFIG_PREFIX + ".nacos";
    String CONFIG_CONSUL_PREFIX = CONFIG_PREFIX + ".consul";
    String CONFIG_ETCD3_PREFIX = CONFIG_PREFIX + ".etcd3";
    String CONFIG_APOLLO_PREFIX = CONFIG_PREFIX + ".apollo";
    String CONFIG_ZK_PREFIX = CONFIG_PREFIX + ".zk";
    String CONFIG_FILE_PREFIX = CONFIG_PREFIX + ".file";

    HashMap<String, Class> PROPERTY_MAP = new HashMap<String, Class>(MAP_CAPACITY) {
        private static final long serialVersionUID = -8902807645596274597L;

        {
            put(CLIENT_RM_PREFIX, RmProperties.class);
            put(CLIENT_TM_PREFIX, TmProperties.class);
            put(LOCK_PREFIX, LockProperties.class);
            put(SERVICE_PREFIX, ServiceProperties.class);
            put(SHUTDOWN_PREFIX, ShutdownProperties.class);
            put(THREAD_FACTORY_PREFIX, ThreadFactoryProperties.class);
            put(UNDO_PREFIX, UndoProperties.class);
            put(LOG_PREFIX, LogProperties.class);
            put(TRANSPORT_PREFIX, TransportProperties.class);
            put(CONFIG_PREFIX, ConfigProperties.class);
            put(CONFIG_FILE_PREFIX, ConfigFileProperties.class);
            put(REGISTRY_PREFIX, RegistryProperties.class);

            put(CONFIG_NACOS_PREFIX, ConfigNacosProperties.class);
            put(CONFIG_CONSUL_PREFIX, ConfigConsulProperties.class);
            put(CONFIG_ZK_PREFIX, ConfigZooKeeperProperties.class);
            put(CONFIG_APOLLO_PREFIX, ConfigApolloProperties.class);
            put(CONFIG_ETCD3_PREFIX, ConfigEtcd3Properties.class);

            put(REGISTRY_CONSUL_PREFIX, RegistryConsulProperties.class);
            put(REGISTRY_ETCD3_PREFIX, RegistryEtcd3Properties.class);
            put(REGISTRY_EUREKA_PREFIX, RegistryEurekaProperties.class);
            put(REGISTRY_NACOS_PREFIX, RegistryNacosProperties.class);
            put(REGISTRY_REDIS_PREFIX, RegistryRedisProperties.class);
            put(REGISTRY_SOFA_PREFIX, RegistrySofaProperties.class);
            put(REGISTRY_ZK_PREFIX, RegistryZooKeeperProperties.class);
        }

    };


    /**
     * The following special keys need to be normalized.
     */
    String SPECIAL_KEY_GROUPLIST = "grouplist";
    String SPECIAL_KEY_VGROUP_MAPPING = "vgroupMapping";
}
