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
import java.util.Collections;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.DataSourceProxyTest;
import org.apache.seata.rm.datasource.mock.MockDriver;
import org.apache.seata.sqlparser.struct.ColumnMeta;
import org.apache.seata.sqlparser.struct.IndexMeta;
import org.apache.seata.sqlparser.struct.IndexType;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.sqlparser.struct.TableMetaCache;
import org.apache.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;


public class SqlServerTableMetaCacheTest {
    private static Object[][] columnMetas =
            new Object[][]{
                    new Object[]{"", "", "st1", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
                    new Object[]{"", "", "st1", "name1", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES",
                            "NO"},
                    new Object[]{"", "", "st1", "name2", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 3, "YES",
                            "NO"},
                    new Object[]{"", "", "st1", "name3", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 4, "YES",
                            "NO"}
            };
    private static Object[][] indexMetas =
            new Object[][]{
                    new Object[]{"id", "id", false, "", 3, 0, "A", 34L},
                    new Object[]{"name1", "name1", false, "", 3, 1, "A", 34L},
                    new Object[]{"name2", "name2", true, "", 3, 2, "A", 34L},
            };

    private static Object[][] pkMetas =
            new Object[][]{
                    new Object[]{"id"}
            };

    private static Object[][] tableMetas =
            new Object[][]{
                    new Object[]{"", "m", "st1"}
            };

    private TableMetaCache getTableMetaCache() {
        return TableMetaCacheFactory.getTableMetaCache(JdbcConstants.SQLSERVER);
    }

    @Test
    public void testTableMeta() {
        TableMetaCache tableMetaCache = getTableMetaCache();
        Assertions.assertNotNull(tableMetaCache);
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> tableMetaCache.getTableMeta(null, null, null));
    }

    /**
     * The table meta fetch test.
     */
    @Test
    public void getTableMetaTest_0() throws SQLException {

        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);

        TableMeta tableMeta = getTableMetaCache().getTableMeta(proxy.getPlainConnection(), "m.st1", proxy.getResourceId());

        Assertions.assertEquals("m.st1", tableMeta.getTableName());
        Assertions.assertEquals("m.st1", tableMeta.getOriginalTableName());
        Assertions.assertEquals("id", tableMeta.getPrimaryKeyOnlyName().get(0));

        Assertions.assertEquals("id", tableMeta.getColumnMeta("id").getColumnName());
        Assertions.assertEquals("id", tableMeta.getAutoIncreaseColumn().getColumnName());
        Assertions.assertEquals(1, tableMeta.getPrimaryKeyMap().size());
        Assertions.assertEquals(Collections.singletonList("id"), tableMeta.getPrimaryKeyOnlyName());

        Assertions.assertEquals(columnMetas.length, tableMeta.getAllColumns().size());

        assertColumnMetaEquals(columnMetas[0], tableMeta.getAllColumns().get("id"));
        assertColumnMetaEquals(columnMetas[1], tableMeta.getAllColumns().get("name1"));
        assertColumnMetaEquals(columnMetas[2], tableMeta.getAllColumns().get("name2"));
        assertColumnMetaEquals(columnMetas[3], tableMeta.getAllColumns().get("name3"));

        Assertions.assertEquals(indexMetas.length, tableMeta.getAllIndexes().size());

        assertIndexMetaEquals(indexMetas[0], tableMeta.getAllIndexes().get("id"));
        Assertions.assertEquals(IndexType.PRIMARY, tableMeta.getAllIndexes().get("id").getIndextype());
        assertIndexMetaEquals(indexMetas[1], tableMeta.getAllIndexes().get("name1"));
        Assertions.assertEquals(IndexType.UNIQUE, tableMeta.getAllIndexes().get("name1").getIndextype());

        indexMetas =
                new Object[][]{
                };
        mockDriver.setMockIndexMetasReturnValue(indexMetas);
        Assertions.assertThrows(ShouldNeverHappenException.class, () -> {
            getTableMetaCache().getTableMeta(proxy.getPlainConnection(), "st2", proxy.getResourceId());
        });

        mockDriver.setMockColumnsMetasReturnValue(null);
        Assertions.assertThrows(ShouldNeverHappenException.class, () -> {
            getTableMetaCache().getTableMeta(proxy.getPlainConnection(), "st2", proxy.getResourceId());
        });

        //can not cover the way to get from connection because the mockConnection not support
    }

    private void assertColumnMetaEquals(Object[] expected, ColumnMeta actual) {
        Assertions.assertEquals(expected[0], actual.getTableCat());
        Assertions.assertEquals(expected[3], actual.getColumnName());
        Assertions.assertEquals(expected[4], actual.getDataType());
        Assertions.assertEquals(expected[5], actual.getDataTypeName());
        Assertions.assertEquals(expected[6], actual.getColumnSize());
        Assertions.assertEquals(expected[7], actual.getDecimalDigits());
        Assertions.assertEquals(expected[8], actual.getNumPrecRadix());
        Assertions.assertEquals(expected[9], actual.getNullAble());
        Assertions.assertEquals(expected[10], actual.getRemarks());
        Assertions.assertEquals(expected[11], actual.getColumnDef());
        Assertions.assertEquals(expected[12], actual.getSqlDataType());
        Assertions.assertEquals(expected[13], actual.getSqlDatetimeSub());
        Assertions.assertEquals(expected[14], actual.getCharOctetLength());
        Assertions.assertEquals(expected[15], actual.getOrdinalPosition());
        Assertions.assertEquals(expected[16], actual.getIsNullAble());
        Assertions.assertEquals(expected[17], actual.getIsAutoincrement());
    }

    private void assertIndexMetaEquals(Object[] expected, IndexMeta actual) {
        Assertions.assertEquals(expected[0], actual.getIndexName());
        Assertions.assertEquals(expected[3], actual.getIndexQualifier());
        Assertions.assertEquals(expected[4], (int) actual.getType());
        Assertions.assertEquals(expected[5], actual.getOrdinalPosition());
        Assertions.assertEquals(expected[6], actual.getAscOrDesc());
        Assertions.assertEquals(expected[7], actual.getCardinality());
    }
}
