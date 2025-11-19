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
import org.apache.seata.sqlparser.struct.ColumnMeta;
import org.apache.seata.sqlparser.struct.IndexMeta;
import org.apache.seata.sqlparser.struct.IndexType;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.sqlparser.struct.TableMetaCache;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Types;

import static org.junit.jupiter.api.Assertions.*;

/**
 */
public class OracleTableMetaCacheTest {

    private static Object[][] columnMetas = new Object[][] {
        new Object[] {"", "", "ot1", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
        new Object[] {"", "", "ot1", "name1", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
        new Object[] {"", "", "ot1", "name2", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 3, "YES", "NO"},
        new Object[] {"", "", "ot1", "name3", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 4, "YES", "NO"}
    };

    private static Object[][] indexMetas = new Object[][] {
        new Object[] {"id", "id", false, "", 3, 0, "A", 34},
        new Object[] {"name1", "name1", false, "", 3, 1, "A", 34},
        new Object[] {"name2", "name2", true, "", 3, 2, "A", 34},
    };

    private static Object[][] pkMetas = new Object[][] {new Object[] {"id"}};

    private static Object[][] tableMetas = new Object[][] {new Object[] {"", "t", "ot1"}};

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

    @Test
    public void testGetTableMetaCaching() throws SQLException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE);

        TableMeta tableMeta1 = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.ot1", proxy.getResourceId());
        TableMeta tableMeta2 = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.OT1", proxy.getResourceId());

        Assertions.assertSame(tableMeta1, tableMeta2, "Cache should return same instance");
    }

    @Test
    public void testGetTableMetaWithNoIndexThrowsException() throws SQLException {
        Object[][] emptyIndexMetas = new Object[][] {};
        Object[][] tableMetasForTest = new Object[][] {new Object[] {"", "t", "ot2"}};

        MockDriver mockDriver = new MockDriver(columnMetas, emptyIndexMetas, pkMetas, tableMetasForTest);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE);

        Assertions.assertThrows(ShouldNeverHappenException.class, () -> {
            tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.ot2", proxy.getResourceId());
        });
    }

    @Test
    public void testGetTableMetaWithLowerCaseColumns() throws SQLException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.ot1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        ColumnMeta column = tableMeta.getColumnMeta("name1");
        Assertions.assertNotNull(column);
        Assertions.assertEquals("name1", column.getColumnName());
    }

    @Test
    public void testGetTableMetaWithDuplicateColumnNames() throws SQLException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.ot1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        Assertions.assertEquals(4, tableMeta.getAllColumns().size());
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
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.ot1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        Assertions.assertTrue(tableMeta.getAllIndexes().size() >= 1);
        Assertions.assertEquals(4, tableMeta.getAllColumns().size());
    }

    @Test
    public void testGetTableMetaWithPrimaryKeyIndex() throws SQLException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.ot1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        IndexMeta pkIndex = tableMeta.getAllIndexes().get("id");
        Assertions.assertNotNull(pkIndex);
        Assertions.assertEquals(IndexType.PRIMARY, pkIndex.getIndextype());
    }

    @Test
    public void testGetTableMetaWithUniqueIndex() throws SQLException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.ot1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        IndexMeta uniqueIndex = tableMeta.getAllIndexes().get("name1");
        Assertions.assertNotNull(uniqueIndex);
        Assertions.assertEquals(IndexType.UNIQUE, uniqueIndex.getIndextype());
    }

    @Test
    public void testGetTableMetaWithNormalIndex() throws SQLException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.ot1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        IndexMeta normalIndex = tableMeta.getAllIndexes().get("name2");
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
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.ot1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        Assertions.assertTrue(tableMeta.getAllIndexes().size() >= 1);
    }

    @Test
    public void testGetTableMetaWithPrimaryKeyConstraintDifferentName() throws SQLException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.ot1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        ColumnMeta pkColumn = tableMeta.getPrimaryKeyMap().get("id");
        Assertions.assertNotNull(pkColumn);
        Assertions.assertEquals("id", pkColumn.getColumnName());
        Assertions.assertTrue(tableMeta.getAllIndexes().size() >= 1);
    }

    @Test
    public void testGetTableMetaOriginalTableNamePreserved() throws SQLException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE);

        String originalName = "t.ot1";
        TableMeta tableMeta =
                tableMetaCache.getTableMeta(proxy.getPlainConnection(), originalName, proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        Assertions.assertEquals(originalName, tableMeta.getOriginalTableName());
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
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.ot1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        Assertions.assertTrue(tableMeta.getPrimaryKeyMap().size() >= 1);
        Assertions.assertTrue(tableMeta.getAllIndexes().size() >= 1);
    }

    @Test
    public void testGetTableMetaWithQuotedTableName() throws SQLException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE);

        TableMeta tableMeta =
                tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.\"ot1\"", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        Assertions.assertEquals(4, tableMeta.getAllColumns().size());
    }

    @Test
    public void testGetTableMetaAllColumnsPresent() throws SQLException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas, pkMetas, tableMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
        TableMetaCache tableMetaCache = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE);

        TableMeta tableMeta = tableMetaCache.getTableMeta(proxy.getPlainConnection(), "t.ot1", proxy.getResourceId());

        Assertions.assertNotNull(tableMeta);
        Assertions.assertEquals(4, tableMeta.getAllColumns().size());
        Assertions.assertNotNull(tableMeta.getColumnMeta("id"));
        Assertions.assertNotNull(tableMeta.getColumnMeta("name1"));
        Assertions.assertNotNull(tableMeta.getColumnMeta("name2"));
        Assertions.assertNotNull(tableMeta.getColumnMeta("name3"));
    }
}
