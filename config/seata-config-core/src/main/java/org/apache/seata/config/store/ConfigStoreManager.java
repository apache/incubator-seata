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

import org.apache.seata.config.ConfigurationChangeListener;

import java.util.Map;

/**
 * The interface Local config store manager.
 *
 */
public interface ConfigStoreManager {
    String get(String group, String key);

    Map<String, Object> getAll(String group);

    Boolean put(String group, String key, Object value);

    Boolean delete(String group, String key);

    Boolean putAll(String group, Map<String, Object> configMap);

    Boolean deleteAll(String group);

    Boolean isEmpty(String group);

    Map<String, Object> getConfigMap();

    Boolean putConfigMap(Map<String, Object> configMap);

    Boolean clearData();
    void destroy();

    default void addConfigListener(String group, String dataId, ConfigurationChangeListener listener) {};

    default void removeConfigListener(String group, String dataId, ConfigurationChangeListener listener) {};


}
