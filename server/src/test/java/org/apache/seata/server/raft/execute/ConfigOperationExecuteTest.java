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
package org.apache.seata.server.raft.execute;


import java.util.LinkedHashMap;

import javax.annotation.Resource;

import org.apache.seata.common.util.NetUtil;
import org.apache.seata.config.dto.ConfigurationInfoDto;
import org.apache.seata.config.dto.ConfigurationItem;
import org.apache.seata.config.store.rocksdb.RocksDBConfigStoreManager;
import org.apache.seata.server.cluster.raft.RaftConfigServerManager;
import org.apache.seata.server.cluster.raft.execute.config.ConfigOperationExecute;
import org.apache.seata.server.cluster.raft.processor.response.ConfigOperationResponse;
import org.apache.seata.server.controller.ClusterController;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;


@SpringBootTest
class ConfigOperationExecuteTest {
    @Resource
    private ClusterController clusterController;

    private static final String NAMESPACE = "test";
    private static final String DATA_ID = "test";

    @BeforeAll
    public static void setUp(ApplicationContext context) {
        RaftConfigServerManager.destroy();
        System.setProperty("server.raft.serverAddr", NetUtil.getLocalIp() + ":9091");
        System.setProperty("config.type", "raft");
        System.setProperty("registry.preferredNetworks", "*");
        System.setProperty("config.raft.db.dir", "configStore");
        System.setProperty("config.raft.db.destroyOnShutdown", "true");
        RaftConfigServerManager.init();
        RaftConfigServerManager.start();
    }

    @AfterAll
    public static void destroy() {
        RaftConfigServerManager.destroy();
        System.clearProperty("server.raft.serverAddr");
        System.clearProperty("config.type");
        System.clearProperty("registry.preferredNetworks");
        System.clearProperty("config.raft.db.dir");
        System.clearProperty("config.raft.db.destroyOnShutdown");
    }

    @Test
    public void testCRUD() {
        clusterController.deleteAllConfig(NAMESPACE, DATA_ID);
        String key1 = "aaa";
        String value1 = "bbb";
        String key2 = "ccc";
        String value2 = "ddd";
        Assertions.assertTrue(clusterController.getConfig(NAMESPACE,  DATA_ID, key1).isSuccess());
        Assertions.assertTrue(clusterController.getAllConfig(NAMESPACE,  DATA_ID).isSuccess());
        Assertions.assertTrue(clusterController.putConfig(NAMESPACE, DATA_ID, key1, value1).isSuccess());
        Assertions.assertTrue(clusterController.putConfig(NAMESPACE, DATA_ID, key2, value2).isSuccess());
        Assertions.assertTrue(clusterController.deleteConfig(NAMESPACE, DATA_ID, key1).isSuccess());
        Assertions.assertTrue(clusterController.deleteAllConfig(NAMESPACE, DATA_ID).isSuccess());
        Assertions.assertTrue(clusterController.getNamespaces().isSuccess());
        Assertions.assertTrue(clusterController.getDataIds(NAMESPACE).isSuccess());
    }
}
