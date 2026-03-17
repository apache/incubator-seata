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
package org.apache.seata.core.rpc.netty;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.seata.core.protocol.SlowSqlEntry;
import org.apache.seata.core.protocol.SqlExecutionEntry;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The Sql Collector for HikariCP and Druid.
 *
 */
public class SqlCollector {

    private static final int SQL_CACHE_MAX_SIZE = 8192;
    private static final int SQL_CACHE_EXPIRE_DAYS = 3;
    private static final long DEFAULT_SLOW_SQL_THRESHOLD_MILLIS = 5000L;
    private static volatile long slowSqlThresholdMillis = DEFAULT_SLOW_SQL_THRESHOLD_MILLIS;

    private static final Cache<String, SqlExecutionEntry> ALL_SQL_CACHE = CacheBuilder.newBuilder()
            .maximumSize(SQL_CACHE_MAX_SIZE)
            .expireAfterWrite(SQL_CACHE_EXPIRE_DAYS, TimeUnit.DAYS)
            .build();

    private static final Cache<String, SlowSqlEntry> SLOW_SQL_CACHE = CacheBuilder.newBuilder()
            .maximumSize(SQL_CACHE_MAX_SIZE)
            .expireAfterWrite(SQL_CACHE_EXPIRE_DAYS, TimeUnit.DAYS)
            .build();

    private SqlCollector() {}

    public static void setSlowSqlThresholdMillis(long threshold) {
        slowSqlThresholdMillis = threshold;
    }

    public static void addSqlExecutionEntry(
            String sql, long executionTimeMillis, long holdTimeMillis, LocalDateTime timestamp) {
        ALL_SQL_CACHE.put(sql, new SqlExecutionEntry(sql, executionTimeMillis, holdTimeMillis, timestamp));
    }

    public static void addSlowSqlEntry(String sql, long executionTimeMillis, LocalDateTime timestamp) {
        if (executionTimeMillis >= slowSqlThresholdMillis) {
            SlowSqlEntry slowSqlEntry = new SlowSqlEntry(sql, executionTimeMillis, timestamp);
            SLOW_SQL_CACHE.put(sql, slowSqlEntry);
        }
    }

    // Package-private accessors for DataSourceConnectionPoolCollector
    static Map<String, SqlExecutionEntry> getAllSqlMap() {
        return ALL_SQL_CACHE.asMap();
    }

    static Map<String, SlowSqlEntry> getSlowSqlMap() {
        return SLOW_SQL_CACHE.asMap();
    }
}
