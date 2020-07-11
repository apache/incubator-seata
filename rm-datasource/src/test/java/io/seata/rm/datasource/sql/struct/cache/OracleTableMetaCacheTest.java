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

import java.sql.SQLException;
import java.sql.Types;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.util.JdbcConstants;

import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.mock.MockDriver;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableMetaCache;
import io.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
  * @author will.zjw
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

    @Test
    public void getTableMetaTest() throws SQLException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = new DataSourceProxy(dataSource);

        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.ot1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);

        tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.\"ot1\"", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
    }
}
