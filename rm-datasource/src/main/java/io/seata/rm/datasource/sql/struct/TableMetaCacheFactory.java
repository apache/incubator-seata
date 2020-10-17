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
package io.seata.rm.datasource.sql.struct;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;

/**
 * @author guoyao
 */
public class TableMetaCacheFactory {

    private static final Map<String, TableMetaCache> TABLE_META_CACHE_MAP = new ConcurrentHashMap<>();

    /**
     * get table meta cache
     *
     * @param dbType the db type
     * @return table meta cache
     */
    public static TableMetaCache getTableMetaCache(String dbType) {
        return CollectionUtils.computeIfAbsent(TABLE_META_CACHE_MAP, dbType,
            key -> EnhancedServiceLoader.load(TableMetaCache.class, dbType));
    }
}
