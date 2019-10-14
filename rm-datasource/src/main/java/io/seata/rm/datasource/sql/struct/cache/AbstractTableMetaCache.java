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
package io.seata.rm.datasource.sql.struct.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableMetaCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * The type Table meta cache.
 *
 * @author sharajava
 */
public abstract class AbstractTableMetaCache implements TableMetaCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTableMetaCache.class);

    private static final long CACHE_SIZE = 100000;

    private static final long EXPIRE_TIME = 900 * 1000;

    private static final Cache<String, TableMeta> TABLE_META_CACHE = Caffeine.newBuilder().maximumSize(CACHE_SIZE)
            .expireAfterWrite(EXPIRE_TIME, TimeUnit.MILLISECONDS).softValues().build();


    @Override
    public TableMeta getTableMeta(final DataSourceProxy dataSourceProxy, final String tableName) {
        if (StringUtils.isNullOrEmpty(tableName)) {
            throw new IllegalArgumentException("TableMeta cannot be fetched without tableName");
        }

        TableMeta tmeta;
        final String key = getCacheKey(dataSourceProxy, tableName);
        tmeta = TABLE_META_CACHE.get(key, mappingFunction -> {
            try {
                return fetchSchema(dataSourceProxy.getTargetDataSource(), tableName);
            } catch (SQLException e) {
                LOGGER.error("get cache error:{}", e.getMessage(), e);
                return null;
            }
        });

        if (tmeta == null) {
            try {
                tmeta = fetchSchema(dataSourceProxy.getTargetDataSource(), tableName);
            } catch (SQLException e) {
                LOGGER.error("get table meta error:{}", e.getMessage(), e);
            }
        }

        if (tmeta == null) {
            throw new ShouldNeverHappenException(String.format("[xid:%s]get tablemeta failed", RootContext.getXID()));
        }
        return tmeta;
    }

    @Override
    public void refresh(final DataSourceProxy dataSourceProxy) {
        ConcurrentMap<String, TableMeta> tableMetaMap = TABLE_META_CACHE.asMap();
        for (Map.Entry<String, TableMeta> entry : tableMetaMap.entrySet()) {
            String key = getCacheKey(dataSourceProxy, entry.getValue().getTableName());
            if (entry.getKey().equals(key)) {
                try {
                    TableMeta tableMeta = fetchSchema(dataSourceProxy, entry.getValue().getTableName());
                    if (!tableMeta.equals(entry.getValue())) {
                        TABLE_META_CACHE.put(entry.getKey(), tableMeta);
                        LOGGER.info("table meta change was found, update table meta cache automatically.");
                    }
                } catch (SQLException e) {
                    LOGGER.error("get table meta error:{}", e.getMessage(), e);
                }
            }
        }
    }


    /**
     * generate cache key
     *
     * @param dataSourceProxy
     * @param tableName
     * @return
     */
    private String getCacheKey(DataSourceProxy dataSourceProxy, String tableName) {
        return dataSourceProxy.getResourceId() + "." + tableName;
    }

    /**
     * get scheme from datasource and tableName
     *
     * @param dataSource
     * @param tableName
     * @return
     * @throws SQLException
     */
    protected abstract TableMeta fetchSchema(DataSource dataSource, String tableName) throws SQLException;

}
