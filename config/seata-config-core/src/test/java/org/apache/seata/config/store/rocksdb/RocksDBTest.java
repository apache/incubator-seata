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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.apache.seata.common.Constants.DEFAULT_STORE_DATA_ID;
import static org.apache.seata.common.Constants.DEFAULT_STORE_NAMESPACE;


class RocksDBTest {
    private static RocksDBConfigStoreManager configStoreManager;

    private static final String dataId = DEFAULT_STORE_DATA_ID;
    private static final String namespace = DEFAULT_STORE_NAMESPACE;
    @BeforeAll
    static void setUp() {
        configStoreManager = RocksDBConfigStoreManager.getInstance();
    }

    @AfterAll
    static void tearDown() {
        if (configStoreManager != null) {
            configStoreManager.shutdown();
            configStoreManager.destroy();
        }
    }

    @Test
    void getConfigStoreManagerTest() {
        Assertions.assertNotNull(configStoreManager);
    }


    @Test
    void crudTest() {
        configStoreManager.deleteAll(namespace, dataId);
        String key = "aaa";
        String value = "bbb";
        String updateValue = "ccc";
        Assertions.assertTrue(configStoreManager.put(namespace, dataId, key, value));
        Assertions.assertEquals(value, configStoreManager.get(namespace, dataId, key));
        Assertions.assertTrue(configStoreManager.put(namespace, dataId, key, updateValue));
        Assertions.assertEquals(updateValue, configStoreManager.get(namespace, dataId, key));
        Assertions.assertTrue(configStoreManager.delete(namespace, dataId, key));
        Assertions.assertNull(configStoreManager.get(namespace, dataId, key));

    }

    @Test
    void uploadConfigTest() {
        configStoreManager.deleteAll(namespace, dataId);
        HashMap<String, Object> uploadConfigs = new HashMap<>();
        uploadConfigs.put("aaa","111");
        uploadConfigs.put("bbb","222");
        Assertions.assertTrue(configStoreManager.putAll(namespace, dataId, uploadConfigs));
        Assertions.assertEquals(uploadConfigs, configStoreManager.getAll(namespace, dataId));
        configStoreManager.deleteAll(namespace, dataId);
        Assertions.assertTrue(configStoreManager.isEmpty(namespace, dataId));
    }


    @Test
    void multiGroupTest() {
        configStoreManager.deleteAll(namespace, dataId);
        String group1 = "group1";
        String group2 = "group2";
        String key = "aaa";
        String value1 = "aaa";
        String value2 = "bbb";
        // put and get
        Assertions.assertTrue(configStoreManager.put(namespace, group1, key, value1));
        Assertions.assertTrue(configStoreManager.put(namespace, group2, key, value2));
        Assertions.assertEquals(value1, configStoreManager.get(namespace, group1, key));
        Assertions.assertEquals(value2, configStoreManager.get(namespace, group2, key));

        // delete
        Assertions.assertTrue(configStoreManager.delete(namespace, group1, key));
        Assertions.assertTrue(configStoreManager.delete(namespace, group2, key));
        Assertions.assertNull(configStoreManager.get(namespace, group1, key));
        Assertions.assertNull(configStoreManager.get(namespace, group2, key));
    }


    @Test
    void multiNamespaceAndGroupTest() {
        configStoreManager.clearData();
        String namespace1 = "namespace1";
        String namespace2 = "namespace2";
        List<String> namespaces = Arrays.asList(DEFAULT_STORE_NAMESPACE, namespace1, namespace2);
        String dataId1 = "dataId1";
        String dataId2 = "dataId2";
        List<String> dataIds = Arrays.asList(dataId1, dataId2);
        String key = "aaa";
        // put and get
        Assertions.assertTrue(configStoreManager.put(namespace1, dataId1, key , "11"));
        Assertions.assertTrue(configStoreManager.put(namespace1, dataId2, key , "12"));
        Assertions.assertTrue(configStoreManager.put(namespace2, dataId1, key , "21"));
        Assertions.assertTrue(configStoreManager.put(namespace2, dataId2, key , "22"));
        Assertions.assertEquals("11", configStoreManager.get(namespace1, dataId1, key));
        Assertions.assertEquals("12", configStoreManager.get(namespace1, dataId2, key));
        Assertions.assertEquals("21", configStoreManager.get(namespace2, dataId1, key));
        Assertions.assertEquals("22", configStoreManager.get(namespace2, dataId2, key));
        Assertions.assertEquals(namespaces.size(), configStoreManager.getAllNamespaces().size());
        Assertions.assertEquals(dataIds.size(), configStoreManager.getAllDataIds(namespace1).size());
        Assertions.assertEquals(dataIds.size(), configStoreManager.getAllDataIds(namespace2).size());
        // delete
        Assertions.assertTrue(configStoreManager.delete(namespace1, dataId1, key));
        Assertions.assertTrue(configStoreManager.delete(namespace1, dataId2, key));
        Assertions.assertTrue(configStoreManager.delete(namespace2, dataId1, key));
        Assertions.assertTrue(configStoreManager.delete(namespace2, dataId2, key));
        Assertions.assertNull(configStoreManager.get(namespace1, dataId1, key));
        Assertions.assertNull(configStoreManager.get(namespace1, dataId2, key));
        Assertions.assertNull(configStoreManager.get(namespace2, dataId1, key));
        Assertions.assertNull(configStoreManager.get(namespace2, dataId2, key));
    }

    @Test
    void uploadTest() {
        configStoreManager.clearData();
        String namespace1 = "namespace1";
        String namespace2 = "namespace2";
        String dataId1 = "dataId1";
        String dataId2 = "dataId2";
        HashMap<String, Map<String, Object>> configMap = new HashMap<String, Map<String, Object>>();
        HashMap<String, Object> map1 = new HashMap<String, Object>() {{
            put(dataId1, "11");
            put(dataId2, "12");
        }};
        HashMap<String, Object> map2 = new HashMap<String, Object>() {{
            put(dataId1, "21");
            put(dataId2, "22");
        }};
        configMap.put(namespace1,map1);
        configMap.put(namespace2,map2);
        // ensure default namespace
        configMap.put("default",new HashMap<>());
        Assertions.assertTrue(configStoreManager.putConfigMap(configMap));
        Map<String, Map<String, Object>> other = configStoreManager.getConfigMap();

        Assertions.assertEquals(configMap.get(namespace1), other.get(namespace1));
        Assertions.assertEquals(configMap.get(namespace2), other.get(namespace2));
        Assertions.assertEquals(configMap.get("default"), other.get("default"));

        Assertions.assertDoesNotThrow(()->configStoreManager.getAll(namespace1, dataId1));
    }

    @Test
    void configVersionTest() {
        configStoreManager.clearData();
        Long version = 0L;

        String key = "aaa";
        String value = "bbb";
        String newValue = "ccc";

        Assertions.assertTrue(configStoreManager.put(namespace, dataId, key, value));
        version++;
        Assertions.assertEquals(version, configStoreManager.getConfigVersion(namespace, dataId));

        Assertions.assertTrue(configStoreManager.put(namespace, dataId, key, newValue));
        version++;
        Assertions.assertEquals(version, configStoreManager.getConfigVersion(namespace, dataId));

        Assertions.assertTrue(configStoreManager.deleteAll(namespace, dataId));
        Assertions.assertNull(configStoreManager.getConfigVersion(namespace, dataId));
    }
}
