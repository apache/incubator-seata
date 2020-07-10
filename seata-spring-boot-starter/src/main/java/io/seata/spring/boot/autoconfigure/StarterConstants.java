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

/**
 * @author xingfudeshi@gmail.com
 */
public class StarterConstants {
    private static final int MAP_CAPACITY = 64;
    public static final String SEATA_PREFIX = "seata";
    public static final String SEATA_SPRING_CLOUD_ALIBABA_PREFIX = "spring.cloud.alibaba.seata";
    public static final String TRANSPORT_PREFIX = SEATA_PREFIX + ".transport";
    public static final String THREAD_FACTORY_PREFIX_KEBAB_STYLE = TRANSPORT_PREFIX + ".thread-factory";
    public static final String THREAD_FACTORY_PREFIX = TRANSPORT_PREFIX + ".threadFactory";
    public static final String SHUTDOWN_PREFIX = TRANSPORT_PREFIX + ".shutdown";
    public static final String SERVICE_PREFIX = SEATA_PREFIX + ".service";
    public static final String CLIENT_PREFIX = SEATA_PREFIX + ".client";
    public static final String CLIENT_RM_PREFIX = CLIENT_PREFIX + ".rm";
    public static final String CLIENT_TM_PREFIX = CLIENT_PREFIX + ".tm";
    public static final String LOCK_PREFIX = CLIENT_RM_PREFIX + ".lock";
    public static final String UNDO_PREFIX = CLIENT_PREFIX + ".undo";
    public static final String LOG_PREFIX = CLIENT_PREFIX + ".log";

    public static final String REGISTRY_PREFIX = SEATA_PREFIX + ".registry";
    public static final String REGISTRY_NACOS_PREFIX = REGISTRY_PREFIX + ".nacos";
    public static final String REGISTRY_EUREKA_PREFIX = REGISTRY_PREFIX + ".eureka";
    public static final String REGISTRY_REDIS_PREFIX = REGISTRY_PREFIX + ".redis";
    public static final String REGISTRY_ZK_PREFIX = REGISTRY_PREFIX + ".zk";
    public static final String REGISTRY_CONSUL_PREFIX = REGISTRY_PREFIX + ".consul";
    public static final String REGISTRY_ETCD3_PREFIX = REGISTRY_PREFIX + ".etcd3";
    public static final String REGISTRY_SOFA_PREFIX = REGISTRY_PREFIX + ".sofa";

    public static final String CONFIG_PREFIX = SEATA_PREFIX + ".config";
    public static final String CONFIG_NACOS_PREFIX = CONFIG_PREFIX + ".nacos";
    public static final String CONFIG_CONSUL_PREFIX = CONFIG_PREFIX + ".consul";
    public static final String CONFIG_ETCD3_PREFIX = CONFIG_PREFIX + ".etcd3";
    public static final String CONFIG_APOLLO_PREFIX = CONFIG_PREFIX + ".apollo";
    public static final String CONFIG_ZK_PREFIX = CONFIG_PREFIX + ".zk";
    public static final String CONFIG_FILE_PREFIX = CONFIG_PREFIX + ".file";

    public static final HashMap<String, Object> PROPERTY_BEAN_MAP = new HashMap(MAP_CAPACITY);

    /**
     * The following special keys need to be normalized.
     */
    public static final String SPECIAL_KEY_GROUPLIST = "grouplist";
    public static final String SPECIAL_KEY_VGROUP_MAPPING = "vgroupMapping";

}
