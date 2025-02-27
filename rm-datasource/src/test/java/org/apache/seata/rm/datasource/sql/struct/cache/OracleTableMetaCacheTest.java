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
package org.apache.seata.rm.datasource.sql.struct.cache;

import java.sql.SQLException;
import java.sql.Types;

import org.apache.seata.rm.datasource.DataSourceProxyTest;
import org.apache.seata.rm.datasource.mock.MockDriver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.alibaba.druid.pool.DruidDataSource;

import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.sqlparser.struct.TableMetaCache;
import org.apache.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import org.apache.seata.sqlparser.util.JdbcConstants;

/**
  */
public class OracleTableMetaCacheTest {

    private static Object[][] columnMetas =
        new Object[][] {
            new Object[] {"", "", "ot1", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
            new Object[] {"", "", "ot1", "name1", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES",
                "NO"},
            new Object[] {"", "", "ot1", "name2", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 3, "YES",
                "NO"},
            new Object[] {"", "", "ot1", "name3", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 4, "YES",
                "NO"}
        };

    private static Object[][] indexMetas =
        new Object[][] {
            new Object[] {"id", "id", false, "", 3, 0, "A", 34},
            new Object[] {"name1", "name1", false, "", 3, 1, "A", 34},
            new Object[] {"name2", "name2", true, "", 3, 2, "A", 34},
        };

    private static Object[][] pkMetas =
        new Object[][] {
            new Object[] {"id"}
        };

    private static Object[][] tableMetas =
            new Object[][]{
                    new Object[]{"", "t", "ot1"}
            };

    @Test
    public void getTableMetaTest() throws SQLException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);

        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.ot1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        Assertions.assertEquals("OT1", tableMeta.getTableName());
        Assertions.assertEquals("t.ot1", tableMeta.getOriginalTableName());

        tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.\"ot1\"", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
    }
}
