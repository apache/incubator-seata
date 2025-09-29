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
package org.apache.seata.rm.datasource.sql.struct;

import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.mock.MockDataSource;
import org.apache.seata.rm.datasource.sql.struct.cache.MariadbTableMetaCache;
import org.apache.seata.rm.datasource.sql.struct.cache.MysqlTableMetaCache;
import org.apache.seata.rm.datasource.sql.struct.cache.OceanBaseTableMetaCache;
import org.apache.seata.rm.datasource.sql.struct.cache.OracleTableMetaCache;
import org.apache.seata.rm.datasource.sql.struct.cache.PolarDBXTableMetaCache;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

public class TableMetaCacheFactoryTest {

    private static final String NOT_EXIST_SQL_TYPE = "not_exist_sql_type";

    @BeforeEach
    public void clearTableMetaRefreshHolderMap() throws Exception {
        Field field = TableMetaCacheFactory.class.getDeclaredField("TABLE_META_REFRESH_HOLDER_MAP");
        field.setAccessible(true);
        Map<?, ?> map = (Map<?, ?>) field.get(null);
        map.clear();
    }

    @Test
    public void getTableMetaCache() {
        Assertions.assertTrue(
                TableMetaCacheFactory.getTableMetaCache(JdbcConstants.MYSQL) instanceof MysqlTableMetaCache);
        Assertions.assertTrue(
                TableMetaCacheFactory.getTableMetaCache(JdbcConstants.MARIADB) instanceof MariadbTableMetaCache);
        Assertions.assertTrue(
                TableMetaCacheFactory.getTableMetaCache(JdbcConstants.POLARDBX) instanceof PolarDBXTableMetaCache);
        Assertions.assertTrue(
                TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE) instanceof OracleTableMetaCache);
        Assertions.assertTrue(
                TableMetaCacheFactory.getTableMetaCache(JdbcConstants.OCEANBASE) instanceof OceanBaseTableMetaCache);
        Assertions.assertEquals(
                TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE),
                TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE));
        Assertions.assertEquals(
                TableMetaCacheFactory.getTableMetaCache(JdbcConstants.MYSQL),
                TableMetaCacheFactory.getTableMetaCache(JdbcConstants.MYSQL));
        Assertions.assertThrows(EnhancedServiceNotFoundException.class, () -> {
            TableMetaCacheFactory.getTableMetaCache(NOT_EXIST_SQL_TYPE);
        });
    }

    @Test
    public void shutdownTest() throws NoSuchFieldException, IllegalAccessException {
        DataSourceProxy dummy = new DataSourceProxy(new MockDataSource(), "dummy1");
        TableMetaCacheFactory.registerTableMeta(dummy);

        Map<String, TableMetaCacheFactory.TableMetaRefreshHolder> holderMap = getTableMetaRefreshHolderMap();
        Assertions.assertEquals(1, holderMap.size());

        TableMetaCacheFactory.shutdown("jdbc:mysql://127.0.0.1:3306/seata");
        Assertions.assertTrue(holderMap.isEmpty(), "TableMetaRefreshHolder map should be empty after shutdown");
    }

    private Map<String, TableMetaCacheFactory.TableMetaRefreshHolder> getTableMetaRefreshHolderMap()
            throws NoSuchFieldException, IllegalAccessException {
        Field field = TableMetaCacheFactory.class.getDeclaredField("TABLE_META_REFRESH_HOLDER_MAP");
        field.setAccessible(true);
        return (Map<String, TableMetaCacheFactory.TableMetaRefreshHolder>) field.get(null);
    }
}
