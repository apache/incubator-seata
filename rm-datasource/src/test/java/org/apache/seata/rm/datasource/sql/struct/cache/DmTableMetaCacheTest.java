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

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.DataSourceProxyTest;
import org.apache.seata.rm.datasource.mock.MockDriver;
import org.apache.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import org.apache.seata.sqlparser.struct.IndexMeta;
import org.apache.seata.sqlparser.struct.IndexType;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.sqlparser.struct.TableMetaCache;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Types;

public class DmTableMetaCacheTest {

    private static final Object[][] columnMetas = new Object[][] {
        new Object[] {"", "", "dt1", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
        new Object[] {"", "", "dt1", "name1", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
        new Object[] {"", "", "dt1", "name2", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 3, "YES", "NO"},
        new Object[] {"", "", "dt1", "name3", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 4, "YES", "NO"}
    };

    private static final Object[][] indexMetas = new Object[][] {
        new Object[] {"IDX_ID", "id", false, "", 3, 0, "A", 34},
        new Object[] {"IDX_NAME1", "name1", false, "", 3, 1, "A", 34},
        new Object[] {"IDX_NAME2", "name2", true, "", 3, 2, "A", 34},
    };

    private static final Object[][] pkMetas = new Object[][] {new Object[] {"id"}};

    private static final Object[][] tableMetas = new Object[][] {new Object[] {"", "t", "dt1"}};

    @Test
    public void testGetTableMetaBasic() throws SQLException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:dm");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.DM);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.dt1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        Assertions.assertEquals(4, tableMeta.getAllColumns().size());
        Assertions.assertEquals(3, tableMeta.getAllIndexes().size());
    }

    @Test
    public void testGetTableMetaWithSchemaPrefix() throws SQLException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:dm");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.DM);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.dt1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        Assertions.assertEquals("DT1", tableMeta.getTableName());
        Assertions.assertEquals("t.dt1", tableMeta.getOriginalTableName());
    }

    @Test
    public void testGetTableMetaWithQuotedIdentifiers() throws SQLException {
        Object[][] quotedTableMetas = new Object[][] {new Object[] {"", "t", "MixedCase"}};
        Object[][] quotedColumnMetas = new Object[][] {
            new Object[] {
                "", "", "MixedCase", "Id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"
            },
            new Object[] {
                "", "", "MixedCase", "Name", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"
            }
        };
        Object[][] quotedIndexMetas = new Object[][] {new Object[] {"idx_id", "Id", false, "", 3, 0, "A", 34}};
        Object[][] quotedPKMetas = new Object[][] {new Object[] {"Id"}};

        MockDriver mockDriver = new MockDriver(quotedColumnMetas, quotedIndexMetas, quotedPKMetas, quotedTableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:dm");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.DM);

        TableMeta tableMeta =
                tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.\"MixedCase\"", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        Assertions.assertEquals("MixedCase", tableMeta.getTableName());
    }

    @Test
    public void testGetTableMetaThrowsExceptionWhenNoIndex() throws SQLException {
        Object[][] emptyIndexMetas = new Object[][] {};
        Object[][] emptyTableMetas = new Object[][] {new Object[] {"", "t", "dt1"}};

        MockDriver mockDriver = new MockDriver(columnMetas, emptyIndexMetas, pkMetas, emptyTableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:dm");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.DM);

        Assertions.assertThrows(ShouldNeverHappenException.class, () -> {
            tableMetaCache.getTableMeta(proxy.getPlainConnection(), "dt1", proxy.getResourceId());
        });
    }

    @Test
    public void testGetTableMetaWithCompositeIndex() throws SQLException {
        Object[][] compositeIndexMetas = new Object[][] {
            new Object[] {"idx_pk", "id", false, "", 3, 1, "A", 34},
            new Object[] {"idx_composite", "name1", false, "", 3, 1, "A", 34},
            new Object[] {"idx_composite", "name2", false, "", 3, 2, "A", 34}
        };

        MockDriver mockDriver = new MockDriver(columnMetas, compositeIndexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:dm");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.DM);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.dt1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        Assertions.assertTrue(tableMeta.getAllIndexes().size() >= 1);
        Assertions.assertEquals(4, tableMeta.getAllColumns().size());
    }

    @Test
    public void testGetTableMetaWithPrimaryKeyIndex() throws SQLException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:dm");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.DM);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.dt1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        IndexMeta pkIndex = tableMeta.getAllIndexes().get("IDX_ID");
        Assertions.assertNotNull(pkIndex);
        Assertions.assertEquals(IndexType.PRIMARY, pkIndex.getIndextype());
    }

    @Test
    public void testGetTableMetaWithUniqueIndex() throws SQLException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:dm");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.DM);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.dt1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        IndexMeta uniqueIndex = tableMeta.getAllIndexes().get("IDX_NAME1");
        Assertions.assertNotNull(uniqueIndex);
        Assertions.assertEquals(IndexType.UNIQUE, uniqueIndex.getIndextype());
    }

    @Test
    public void testGetTableMetaWithNormalIndex() throws SQLException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:dm");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.DM);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.dt1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        IndexMeta normalIndex = tableMeta.getAllIndexes().get("IDX_NAME2");
        Assertions.assertNotNull(normalIndex);
        Assertions.assertEquals(IndexType.NORMAL, normalIndex.getIndextype());
    }

    @Test
    public void testGetTableMetaWithNullIndexName() throws SQLException {
        Object[][] nullIndexMetas = new Object[][] {
            new Object[] {"idx_id", "id", false, "", 3, 0, "A", 34},
            new Object[] {null, "name1", false, "", 3, 1, "A", 34}
        };

        MockDriver mockDriver = new MockDriver(columnMetas, nullIndexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:dm");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.DM);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.dt1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        Assertions.assertTrue(tableMeta.getAllIndexes().containsKey("IDX_ID"));
        Assertions.assertFalse(tableMeta.getAllIndexes().containsKey(null));
    }

    @Test
    public void testGetTableMetaCaching() throws SQLException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:dm");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.DM);

        TableMeta tableMeta1 = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.dt1", proxy.getResourceId());
        TableMeta tableMeta2 = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "T.DT1", proxy.getResourceId());

        Assertions.assertSame(tableMeta1, tableMeta2, "Cache should return same instance");
    }

    @Test
    public void testGetTableMetaWithoutSchemaPrefix() throws SQLException {
        Object[][] singleTableMetas = new Object[][] {new Object[] {"", "public", "dt1"}};

        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas, singleTableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:dm");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.DM);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "dt1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        Assertions.assertEquals("DT1", tableMeta.getTableName());
    }

    @Test
    public void testGetTableMetaWithLowerCaseTable() throws SQLException {
        Object[][] lowerCaseTableMetas = new Object[][] {new Object[] {"", "t", "lowertable"}};
        Object[][] lowerCaseColumnMetas = new Object[][] {
            new Object[] {
                "", "", "lowertable", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"
            }
        };
        Object[][] lowerCaseIndexMetas = new Object[][] {new Object[] {"idx_id", "id", false, "", 3, 0, "A", 34}};
        Object[][] lowerCasePKMetas = new Object[][] {new Object[] {"id"}};

        MockDriver mockDriver =
                new MockDriver(lowerCaseColumnMetas, lowerCaseIndexMetas, lowerCasePKMetas, lowerCaseTableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:dm");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.DM);

        TableMeta tableMeta =
                tableMetaCache.getTableMeta(proxy.getPlainConnection(), "lowertable", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
    }

    @Test
    public void testGetTableMetaWithMultiplePrimaryKeys() throws SQLException {
        Object[][] multiPKMetas = new Object[][] {new Object[] {"id"}, new Object[] {"name1"}};
        Object[][] multiPKIndexMetas = new Object[][] {
            new Object[] {"idx_composite_pk", "id", false, "", 3, 1, "A", 34},
            new Object[] {"idx_composite_pk", "name1", false, "", 3, 2, "A", 34}
        };

        MockDriver mockDriver = new MockDriver(columnMetas, multiPKIndexMetas, multiPKMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:dm");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.DM);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.dt1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        Assertions.assertTrue(tableMeta.getPrimaryKeyMap().size() >= 1);
        Assertions.assertTrue(tableMeta.getAllIndexes().size() >= 1);
    }

    @Test
    public void testGetTableMetaOriginalTableNamePreserved() throws SQLException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:dm");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.DM);

        String originalName = "t.dt1";
        TableMeta tableMeta =
                tableMetaCache.getTableMeta(proxy.getPlainConnection(), originalName, proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        Assertions.assertEquals(originalName, tableMeta.getOriginalTableName());
    }

    @Test
    public void testCompositeIndexWithDifferentColumnOrder() throws SQLException {
        // Regression test for a bug fix: different column order caused List.equals to fail, which could lead to
        // failing to recognize a primary key index.
        // Scenario: composite primary key (ID, USER_ID) while a unique index lists the same columns in reverse order
        // (USER_ID, ID).
        // Before the fix: List.equals returned FALSE because of the different order, causing the primary key index to
        // be unrecognized.
        // After the fix: using Set.equals should return TRUE and correctly identify the index as PRIMARY.

        Object[][] compositeIndexDiffOrder = new Object[][] {
            new Object[] {"idx_id_userid", "id", false, "", 3, 1, "A", 34}, // column order 1
            new Object[] {"idx_id_userid", "user_id", false, "", 3, 2, "A", 34} // column order 2
        };
        Object[][] compositePKMetas = new Object[][] {
            new Object[] {"user_id"}, // PK column order 1 (intentionally different from index order)
            new Object[] {"id"} // PK column order 2
        };
        Object[][] compositeColumnMetas = new Object[][] {
            new Object[] {"", "", "dt2", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"
            },
            new Object[] {
                "", "", "dt2", "user_id", Types.INTEGER, "INTEGER", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"
            }
        };
        Object[][] compositeTableMetas = new Object[][] {new Object[] {"", "t", "dt2"}};

        MockDriver mockDriver =
                new MockDriver(compositeColumnMetas, compositeIndexDiffOrder, compositePKMetas, compositeTableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:dm");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.DM);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.dt2", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        IndexMeta compositeIndex = tableMeta.getAllIndexes().get("idx_id_userid");
        Assertions.assertNotNull(compositeIndex);
        // Key assertion: should be recognized as PRIMARY (after the fix), not mistakenly treated as UNIQUE
        Assertions.assertEquals(
                IndexType.PRIMARY,
                compositeIndex.getIndextype(),
                "Composite index with different column order should be recognized as PRIMARY");
    }

    @Test
    public void testCompositeIndexWithExtraColumnsNotMarkedAsPrimary() throws SQLException {
        // Regression test for a bug fix: a composite unique index that contains primary key columns plus extra columns
        // should NOT be marked as PRIMARY.
        // Scenario: primary key (ID), unique index (ID, NAME) — unique index contains PK column but has extra NAME.
        // Before the fix: the previous DM implementation matched by count (matchCols == pkcol.size()) and could
        // misidentify
        // PRIMARY.
        // After the fix: using Set equality ({ID, NAME} != {ID}) prevents the index from being marked as PRIMARY

        Object[][] mixedIndexMetas = new Object[][] {
            new Object[] {"idx_id", "id", false, "", 3, 1, "A", 34},
            new Object[] {"uk_id_name", "id", false, "", 3, 1, "A", 34}, // unique index contains the primary key column
            new Object[] {"uk_id_name", "name1", false, "", 3, 2, "A", 34} // but it also has extra columns
        };

        // Use a dedicated table name to avoid cache hits from earlier tests
        Object[][] extraColumnMetas = new Object[][] {
            new Object[] {"", "", "dt3", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"
            },
            new Object[] {
                "", "", "dt3", "name1", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"
            }
        };
        Object[][] extraPkMetas = new Object[][] {new Object[] {"id"}};
        Object[][] extraTableMetas = new Object[][] {new Object[] {"", "t", "dt3"}};

        MockDriver mockDriver = new MockDriver(extraColumnMetas, mixedIndexMetas, extraPkMetas, extraTableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:dm");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.DM);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.dt3", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        IndexMeta pkIndex = tableMeta.getAllIndexes().get("idx_id");
        Assertions.assertNotNull(pkIndex);
        Assertions.assertEquals(IndexType.PRIMARY, pkIndex.getIndextype(), "Single column PK index should be PRIMARY");

        IndexMeta mixedIndex = tableMeta.getAllIndexes().get("uk_id_name");
        Assertions.assertNotNull(mixedIndex);
        // Key assertion: should remain UNIQUE (should not be misidentified as PRIMARY)
        Assertions.assertEquals(
                IndexType.UNIQUE,
                mixedIndex.getIndextype(),
                "Composite index with extra columns should NOT be marked as PRIMARY");
    }
}
