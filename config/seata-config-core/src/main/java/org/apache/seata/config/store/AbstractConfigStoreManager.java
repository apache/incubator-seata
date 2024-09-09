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
package org.apache.seata.config.store;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Abstract config store manager.
 *
 */
public abstract class AbstractConfigStoreManager implements ConfigStoreManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConfigStoreManager.class);

    @Override
    public String get(String namespace, String dataId, String key) {
        return null;
    }

    @Override
    public Map<String, Object> getAll(String namespace, String dataId) {
        return new HashMap<>();
    }

    @Override
    public Boolean put(String namespace, String dataId, String key, Object value) {
        return Boolean.FALSE;
    }

    @Override
    public Boolean delete(String namespace, String dataId, String key) {
        return Boolean.FALSE;
    }

    @Override
    public Boolean putAll(String namespace, String dataId, Map<String, Object> configMap) {
        return Boolean.FALSE;
    }

    @Override
    public Boolean deleteAll(String namespace, String dataId) {
        return Boolean.FALSE;
    }

    @Override
    public Boolean isEmpty(String namespace, String dataId) {
        return Boolean.TRUE;
    }

    @Override
    public Map<String, Map<String, Object>> getConfigMap() {
        return new HashMap<>();
    }

    @Override
    public Boolean putConfigMap(Map<String, Map<String, Object>> configMap) {
        return Boolean.FALSE;
    }

    @Override
    public Boolean clearData() {
        return Boolean.FALSE;
    }

    @Override
    public List<String> getAllNamespaces() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getAllDataIds(String namespace) {
        return Collections.emptyList();
    }

    @Override
    public Long getConfigVersion(String namespace, String dataId) {
        return 0L;
    }

    @Override
    public Boolean putConfigVersion(String namespace, String dataId, Long version) {
        return Boolean.FALSE;
    }

    @Override
    public Boolean deleteConfigVersion(String namespace, String dataId) {
        return Boolean.FALSE;
    }

    @Override
    public void destroy() {}

    @Override
    public void shutdown() {}
}
