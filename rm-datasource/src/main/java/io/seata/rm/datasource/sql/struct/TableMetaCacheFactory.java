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

import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.seata.common.ConfigurationKeys;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.CollectionUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.sqlparser.struct.TableMetaCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.common.DefaultValues.DEFAULT_CLIENT_TABLE_META_CHECK_ENABLE;
import static io.seata.common.DefaultValues.DEFAULT_TABLE_META_CHECKER_INTERVAL;

/**
 * Table meta cache factory
 *
 * @author guoyao
 */
public class TableMetaCacheFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(TableMetaCacheFactory.class);

    private static final Map<String, TableMetaCache> TABLE_META_CACHE_MAP = new ConcurrentHashMap<>();

    private static final Map<String, TableMetaRefreshHolder> TABLE_META_REFRESH_HOLDER_MAP = new ConcurrentHashMap<>();

    private static final long TABLE_META_REFRESH_INTERVAL_TIME = 1000L;

    private static final int MAX_QUEUE_SIZE = 2000;

    /**
     * Enable the table meta checker
     */
    private static boolean ENABLE_TABLE_META_CHECKER_ENABLE = ConfigurationFactory.getInstance()
        .getBoolean(ConfigurationKeys.CLIENT_TABLE_META_CHECK_ENABLE, DEFAULT_CLIENT_TABLE_META_CHECK_ENABLE);

    /**
     * Table meta checker interval
     */
    private static final long TABLE_META_CHECKER_INTERVAL = ConfigurationFactory.getInstance()
        .getLong(ConfigurationKeys.CLIENT_TABLE_META_CHECKER_INTERVAL, DEFAULT_TABLE_META_CHECKER_INTERVAL);


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

    /**
     * register table meta
     *
     * @param dataSourceProxy
     */
    public static void registerTableMeta(DataSourceProxy dataSourceProxy) {
        TableMetaRefreshHolder holder = new TableMetaRefreshHolder(dataSourceProxy);
        TABLE_META_REFRESH_HOLDER_MAP.put(dataSourceProxy.getResourceId(), holder);
    }

    /**
     * public tableMeta refresh event
     */
    public static void tableMetaRefreshEvent(String resourceId) {
        TableMetaRefreshHolder refreshHolder = TABLE_META_REFRESH_HOLDER_MAP.get(resourceId);
        boolean offer = refreshHolder.tableMetaRefreshQueue.offer(System.nanoTime());
        if (!offer) {
            LOGGER.error("table refresh event offer error:{}", resourceId);
        }
    }

    static class TableMetaRefreshHolder {
        private long lastRefreshFinishTime;
        private DataSourceProxy dataSource;
        private BlockingQueue<Long> tableMetaRefreshQueue;


        private final Executor tableMetaRefreshExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), new NamedThreadFactory("tableMetaRefresh", 1, true));

        TableMetaRefreshHolder(DataSourceProxy dataSource) {
            this.dataSource = dataSource;
            this.lastRefreshFinishTime = System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(TABLE_META_REFRESH_INTERVAL_TIME);
            this.tableMetaRefreshQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);

            tableMetaRefreshExecutor.execute(() -> {
                while (true) {
                    // 1. check table meta
                    if (ENABLE_TABLE_META_CHECKER_ENABLE 
                        && System.nanoTime() - lastRefreshFinishTime > TimeUnit.MILLISECONDS.toNanos(TABLE_META_CHECKER_INTERVAL)) {
                        tableMetaRefreshEvent(dataSource.getResourceId());
                    }

                    // 2. refresh table meta
                    try {
                        Long eventTime = tableMetaRefreshQueue.poll(TABLE_META_REFRESH_INTERVAL_TIME, TimeUnit.MILLISECONDS);
                        // if it has bean refreshed not long ago, skip
                        if (eventTime != null && eventTime - lastRefreshFinishTime > TimeUnit.MILLISECONDS.toNanos(TABLE_META_REFRESH_INTERVAL_TIME)) {
                            try (Connection connection = dataSource.getConnection()) {
                                TableMetaCache tableMetaCache =
                                    TableMetaCacheFactory.getTableMetaCache(dataSource.getDbType());
                                tableMetaCache.refresh(connection, dataSource.getResourceId());
                            }
                            lastRefreshFinishTime = System.nanoTime();
                        }
                    } catch (Exception exx) {
                        LOGGER.error("table refresh error:{}", exx.getMessage(), exx);
                        // Avoid high CPU usage due to infinite loops caused by database exceptions
                        lastRefreshFinishTime = System.nanoTime();
                    }
                }
            });
        }



    }
}
