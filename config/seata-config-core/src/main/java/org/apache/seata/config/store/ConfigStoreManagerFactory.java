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

import java.util.Objects;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;

import static org.apache.seata.common.ConfigurationKeys.CONFIG_STORE_TYPE;
import static org.apache.seata.common.DefaultValues.DEFAULT_DB_TYPE;

public class ConfigStoreManagerFactory {
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static volatile ConfigStoreManager instance;

    public static ConfigStoreManager getInstance() {
        if (instance == null) {
            synchronized (ConfigStoreManagerFactory.class) {
                if (instance == null) {
                    String dbType = FILE_CONFIG.getConfig(CONFIG_STORE_TYPE, DEFAULT_DB_TYPE);
                    instance = EnhancedServiceLoader.load(ConfigStoreManagerProvider.class, Objects.requireNonNull(dbType), false).provide();
                }
            }
        }
        return instance;
    }

    public static void destroy() {
        if (instance != null) {
            instance.shutdown();
        }
    }
}
