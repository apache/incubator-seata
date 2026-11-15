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
package org.apache.seata.server.storage.file;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.server.storage.file.spi.FileStoreProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for file-mode storage providers.
 */
public final class FileStoreProviderFactory {

    private static final String DEFAULT_PROVIDER = "default";
    private static final Map<String, FileStoreProvider> INSTANCES = new ConcurrentHashMap<>();

    private FileStoreProviderFactory() {}

    /**
     * Gets a file-mode storage provider by name.
     *
     * @param name the provider name
     * @return the cached provider instance
     */
    public static FileStoreProvider getProvider(String name) {
        String providerName = StringUtils.isBlank(name) ? DEFAULT_PROVIDER : name;
        return CollectionUtils.computeIfAbsent(
                INSTANCES, providerName, key -> EnhancedServiceLoader.load(FileStoreProvider.class, key));
    }
}
