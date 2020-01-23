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

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.loader.EnhancedServiceLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author guoyao
 */
public class TableMetaCacheFactory {

    private static volatile Map<String, TableMetaCache> tableMetaCacheMap;

    /**
     * get table meta cache
     *
     * @param dbType the db type
     * @return table meta cache
     */
    public static TableMetaCache getTableMetaCache(String dbType) {
        dbType = dbType.toLowerCase();
        if (tableMetaCacheMap == null) {
            synchronized (TableMetaCacheFactory.class) {
                if (tableMetaCacheMap == null) {
                    Map<String, TableMetaCache> initializedMap = new HashMap<>();
                    List<TableMetaCache> cacheList = EnhancedServiceLoader.loadAll(TableMetaCache.class);
                    for (TableMetaCache cache : cacheList) {
                        initializedMap.put(cache.getDbType().toLowerCase(), cache);
                    }
                    tableMetaCacheMap = initializedMap;
                }
            }
        }
        if (tableMetaCacheMap.containsKey(dbType)) {
            return tableMetaCacheMap.get(dbType);
        }
        throw new NotSupportYetException("not support dbType[" + dbType + "]");
    }
}
