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

import com.alibaba.druid.util.JdbcConstants;
import io.seata.common.exception.NotSupportYetException;
import io.seata.rm.datasource.sql.struct.cache.MysqlTableMetaCache;
import io.seata.rm.datasource.sql.struct.cache.OracleTableMetaCache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author guoyao
 */
public class TableMetaCacheFactoryTest {

    private static final String NOT_EXIST_SQL_TYPE = "not_exist_sql_type";

    @Test
    public void getTableMetaCache() {
        Assertions.assertTrue(TableMetaCacheFactory.getTableMetaCache(JdbcConstants.MYSQL) instanceof MysqlTableMetaCache);
        Assertions.assertTrue(TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE) instanceof OracleTableMetaCache);
        Assertions.assertEquals(TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE), TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE));
        Assertions.assertEquals(TableMetaCacheFactory.getTableMetaCache(JdbcConstants.MYSQL), TableMetaCacheFactory.getTableMetaCache(JdbcConstants.MYSQL));
        Assertions.assertThrows(NotSupportYetException.class, () -> {
            TableMetaCacheFactory.getTableMetaCache(NOT_EXIST_SQL_TYPE);
        });
    }
}
