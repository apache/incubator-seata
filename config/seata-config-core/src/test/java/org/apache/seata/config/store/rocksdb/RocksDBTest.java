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
package org.apache.seata.config.store.rocksdb;

import org.apache.seata.config.ConfigurationFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.util.HashMap;


import static org.apache.seata.common.Constants.DEFAULT_STORE_GROUP;


class RocksDBTest {
    private static RocksDBConfigStoreManager configStoreManager;

    private static String group = DEFAULT_STORE_GROUP;
    @BeforeAll
    static void setUp() {
        configStoreManager = RocksDBConfigStoreManager.getInstance();
    }

    @AfterAll
    static void tearDown() {
        if (configStoreManager != null) {
            configStoreManager.destroy();
        }
    }

    @Test
    void getConfigStoreManagerTest() {
        Assertions.assertNotNull(configStoreManager);
    }


    @Test
    void crudTest() {
        configStoreManager.deleteAll(group);
        String key = "aaa";
        String value = "bbb";
        String updateValue = "ccc";
        Assertions.assertTrue(configStoreManager.put(group, key, value));
        Assertions.assertEquals(value, configStoreManager.get(group, key));
        Assertions.assertTrue(configStoreManager.put(group, key, updateValue));
        Assertions.assertEquals(updateValue, configStoreManager.get(group, key));
        Assertions.assertTrue(configStoreManager.delete(group, key));
        Assertions.assertNull(configStoreManager.get(group, key));

    }

    @Test
    void uploadConfigTest() {
        configStoreManager.deleteAll(group);
        HashMap<String, Object> uploadConfigs = new HashMap<>();
        uploadConfigs.put("aaa","111");
        uploadConfigs.put("bbb","222");
        Assertions.assertTrue(configStoreManager.putAll(group, uploadConfigs));
        Assertions.assertEquals(uploadConfigs, configStoreManager.getAll(group));
        configStoreManager.deleteAll(group);
        Assertions.assertTrue(configStoreManager.isEmpty(group));
    }


    @Test
    void multiGroupTest() {
        configStoreManager.deleteAll(group);
        String group1 = "group1";
        String group2 = "group2";
        String key = "aaa";
        String value1 = "aaa";
        String value2 = "bbb";
        // put and get
        Assertions.assertTrue(configStoreManager.put(group1, key, value1));
        Assertions.assertTrue(configStoreManager.put(group2, key, value2));
        Assertions.assertEquals(value1, configStoreManager.get(group1, key));
        Assertions.assertEquals(value2, configStoreManager.get(group2, key));

        // delete
        Assertions.assertTrue(configStoreManager.delete(group1, key));
        Assertions.assertTrue(configStoreManager.delete(group2, key));
        Assertions.assertNull(configStoreManager.get(group1, key));
        Assertions.assertNull(configStoreManager.get(group2, key));
    }
}
