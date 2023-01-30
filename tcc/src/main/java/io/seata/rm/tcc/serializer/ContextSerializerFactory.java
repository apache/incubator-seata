/*
 * Copyright 1999-2019 Seata.io Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.seata.rm.tcc.serializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;

/**
 * tcc BusinessActionContext serializer factory
 *
 * @author zouwei
 */
public class ContextSerializerFactory {

    private ContextSerializerFactory() {}

    // ContextSerializer cache
    private static final Map<String, ContextSerializer> INSTANCES = new ConcurrentHashMap<>();

    /**
     * Singleton Holder
     */
    enum SingletonHolder {

        INSTANCE {
            @Override
            ContextSerializer getInstance() {
                return ContextSerializerFactory.getInstance(ContextConstants.DEFAULT_SERIALIZER);
            }
        };

        abstract ContextSerializer getInstance();
    }

    /**
     * get ContextSerializer
     * 
     * @return
     */
    public static ContextSerializer getInstance() {
        return SingletonHolder.INSTANCE.getInstance();
    }

    /**
     * get ContextSerializer by name
     * 
     * @param name
     * @return
     */
    public static ContextSerializer getInstance(String name) {
        return CollectionUtils.computeIfAbsent(INSTANCES, name,
            key -> EnhancedServiceLoader.load(ContextSerializer.class, name));
    }
}
