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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Types;
import java.util.Collections;
import com.alibaba.druid.pool.DruidDataSource;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.mock.MockDriver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import javax.sql.DataSource;

/**
 * The table meta fetch test.
 *
 * @author hanwen created at 2019-02-01
 */
public class TableMetaTest {

    private static Object[][] columnMetas =
        new Object[][] {
            new Object[] {"", "", "t1", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
            new Object[] {"", "", "t1", "name1", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES",
                "NO"},
            new Object[] {"", "", "t1", "name2", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 3, "YES",
                "NO"},
            new Object[] {"", "", "t1", "name3", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 4, "YES",
                "NO"}
        };

    private static Object[][] indexMetas =
        new Object[][] {
            new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},
            new Object[] {"name1", "name1", false, "", 3, 1, "A", 34}
        };

    /**
     * The table meta fetch test.
     */
    @Test
    public void getTableMetaTest_0() {

        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy proxy = new DataSourceProxy(dataSource);

        TableMeta tableMeta = TableMetaCache.getTableMeta(proxy, "t1");

        Assertions.assertEquals("t1", tableMeta.getTableName());
        Assertions.assertEquals("id", tableMeta.getPkName());

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

        assertIndexMetaEquals(indexMetas[0], tableMeta.getAllIndexes().get("PRIMARY"));
        Assertions.assertEquals(IndexType.PRIMARY, tableMeta.getAllIndexes().get("PRIMARY").getIndextype());
        assertIndexMetaEquals(indexMetas[1], tableMeta.getAllIndexes().get("name1"));
        Assertions.assertEquals(IndexType.Unique, tableMeta.getAllIndexes().get("name1").getIndextype());

    }

    @Test
    public void refreshTest_0() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas);

        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setUrl("jdbc:mock:xxx");
        druidDataSource.setDriver(mockDriver);

        DataSourceProxy dataSourceProxy = new DataSourceProxy(druidDataSource);

        Method method = TableMetaCache.class.getDeclaredMethod("fetchSchema", DataSource.class, String.class);
        method.setAccessible(true);

        TableMeta cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        TableMeta realTableMeta = (TableMeta) method.invoke(null, dataSourceProxy, "t1");
        Assertions.assertEquals(cacheTableMeta, realTableMeta);

        //change the index cardinality, but the table structure was not change
        indexMetas =
            new Object[][] {
                    new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 35},
                    new Object[] {"name1", "name1", false, "", 3, 1, "A", 35}
            };
        mockDriver.setMockIndexMetasReturnValue(indexMetas);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        realTableMeta = (TableMeta) method.invoke(null, dataSourceProxy, "t1");
        Assertions.assertEquals(cacheTableMeta, realTableMeta);

        //add a new index
        indexMetas =
            new Object[][] {
                new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},
                new Object[] {"name1", "name1", false, "", 3, 1, "A", 34},
                new Object[] {"id_card", "id_card", false, "", 3, 1, "A", 34}
            };
        mockDriver.setMockIndexMetasReturnValue(indexMetas);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        realTableMeta = (TableMeta) method.invoke(null, dataSourceProxy, "t1");
        Assertions.assertNotEquals(cacheTableMeta, realTableMeta);
        TableMetaCache.refresh(dataSourceProxy);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        Assertions.assertEquals(cacheTableMeta, realTableMeta);

        //change the index sort type
        indexMetas =
            new Object[][] {
                new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},
                new Object[] {"name1", "name1", false, "", 3, 1, "A", 34},
                new Object[] {"id_card", "id_card", false, "", 3, 1, "D", 34}
            };
        mockDriver.setMockIndexMetasReturnValue(indexMetas);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        realTableMeta = (TableMeta) method.invoke(null, dataSourceProxy, "t1");
        Assertions.assertNotEquals(cacheTableMeta, realTableMeta);
        TableMetaCache.refresh(dataSourceProxy);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        Assertions.assertEquals(cacheTableMeta, realTableMeta);

        //change the index ordinal position
        indexMetas =
            new Object[][] {
                new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},
                new Object[] {"name1", "name1", false, "", 3, 1, "A", 34},
                new Object[] {"id_card", "id_card", false, "", 3, 2, "D", 34}
            };
        mockDriver.setMockIndexMetasReturnValue(indexMetas);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        realTableMeta = (TableMeta) method.invoke(null, dataSourceProxy, "t1");
        Assertions.assertNotEquals(cacheTableMeta, realTableMeta);
        TableMetaCache.refresh(dataSourceProxy);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        Assertions.assertEquals(cacheTableMeta, realTableMeta);

        //change the index type
        indexMetas =
            new Object[][] {
                new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},
                new Object[] {"name1", "name1", false, "", 3, 1, "A", 34},
                new Object[] {"id_card", "id_card", false, "", 1, 1, "D", 34}
            };
        mockDriver.setMockIndexMetasReturnValue(indexMetas);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        realTableMeta = (TableMeta) method.invoke(null, dataSourceProxy, "t1");
        Assertions.assertNotEquals(cacheTableMeta, realTableMeta);
        TableMetaCache.refresh(dataSourceProxy);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        Assertions.assertEquals(cacheTableMeta, realTableMeta);

        //change the index indexQualifier
        indexMetas =
            new Object[][] {
                new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},
                new Object[] {"name1", "name1", false, "", 3, 1, "A", 34},
                new Object[] {"id_card", "id_card", false, "t", 1, 1, "D", 34}
            };
        mockDriver.setMockIndexMetasReturnValue(indexMetas);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        realTableMeta = (TableMeta) method.invoke(null, dataSourceProxy, "t1");
        Assertions.assertNotEquals(cacheTableMeta, realTableMeta);
        TableMetaCache.refresh(dataSourceProxy);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        Assertions.assertEquals(cacheTableMeta, realTableMeta);

        //change the index name
        indexMetas =
            new Object[][] {
                new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},
                new Object[] {"name1", "name1", false, "", 3, 1, "A", 34},
                new Object[] {"id_card_number", "id_card_number", false, "t", 1, 1, "D", 34}
            };
        mockDriver.setMockIndexMetasReturnValue(indexMetas);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        realTableMeta = (TableMeta) method.invoke(null, dataSourceProxy, "t1");
        Assertions.assertNotEquals(cacheTableMeta, realTableMeta);
        TableMetaCache.refresh(dataSourceProxy);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        Assertions.assertEquals(cacheTableMeta, realTableMeta);

        //clear the index
        indexMetas = new Object[][]{};
        mockDriver.setMockIndexMetasReturnValue(indexMetas);
        Assertions.assertThrows(Exception.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                method.invoke(null, dataSourceProxy, "t1");
            }
        });

        //reset index meta
        indexMetas =
            new Object[][] {
                new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},
                new Object[] {"name1", "name1", false, "", 3, 1, "A", 34},
                new Object[] {"id_card_number", "id_card_number", false, "t", 1, 1, "D", 34}
            };
        mockDriver.setMockIndexMetasReturnValue(indexMetas);

        //add a new column
        columnMetas =
            new Object[][] {
                new Object[] {"", "", "t1", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
                new Object[] {"", "", "t1", "name1", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                new Object[] {"", "", "t1", "name2", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 3, "YES", "NO"},
                new Object[] {"", "", "t1", "name3", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 4, "YES", "NO"},
                new Object[] {"", "", "t1", "id_card", Types.DECIMAL, "DECIMAL", 64, 0, 10, 0, "", "", 0, 0, 64, 5, "YES", "NO"}
            };
        mockDriver.setMockColumnsMetasReturnValue(columnMetas);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        realTableMeta = (TableMeta) method.invoke(null, dataSourceProxy, "t1");
        Assertions.assertNotEquals(cacheTableMeta, realTableMeta);
        TableMetaCache.refresh(dataSourceProxy);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        Assertions.assertEquals(cacheTableMeta, realTableMeta);

        //change the column isAutoincrement
        columnMetas =
            new Object[][] {
                    new Object[] {"", "", "t1", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
                    new Object[] {"", "", "t1", "name1", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                    new Object[] {"", "", "t1", "name2", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 3, "YES", "NO"},
                    new Object[] {"", "", "t1", "name3", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 4, "YES", "NO"},
                new Object[] {"", "", "t1", "id_card", Types.DECIMAL, "DECIMAL", 64, 0, 10, 0, "", "", 0, 0, 64, 5, "YES", "YES"}
            };
        mockDriver.setMockColumnsMetasReturnValue(columnMetas);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        realTableMeta = (TableMeta) method.invoke(null, dataSourceProxy, "t1");
        Assertions.assertNotEquals(cacheTableMeta, realTableMeta);
        TableMetaCache.refresh(dataSourceProxy);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        Assertions.assertEquals(cacheTableMeta, realTableMeta);

        //change the column isNullAble
        columnMetas =
            new Object[][] {
                new Object[] {"", "", "t1", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
                new Object[] {"", "", "t1", "name1", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                new Object[] {"", "", "t1", "name2", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 3, "YES", "NO"},
                new Object[] {"", "", "t1", "name3", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 4, "YES", "NO"},
                new Object[] {"", "", "t1", "id_card", Types.DECIMAL, "DECIMAL", 64, 0, 10, 1, "", "", 0, 0, 64, 5, "NO", "YES"}
            };
        mockDriver.setMockColumnsMetasReturnValue(columnMetas);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        realTableMeta = (TableMeta) method.invoke(null, dataSourceProxy, "t1");
        Assertions.assertNotEquals(cacheTableMeta, realTableMeta);
        TableMetaCache.refresh(dataSourceProxy);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        Assertions.assertEquals(cacheTableMeta, realTableMeta);

        //change the column ordinary position
        columnMetas =
            new Object[][] {
                new Object[] {"", "", "t1", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
                new Object[] {"", "", "t1", "name1", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                new Object[] {"", "", "t1", "name2", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 3, "YES", "NO"},
                new Object[] {"", "", "t1", "name3", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 5, "YES", "NO"},
                new Object[] {"", "", "t1", "id_card", Types.DECIMAL, "DECIMAL", 64, 0, 10, 1, "", "", 0, 0, 64, 4, "NO", "YES"}
            };
        mockDriver.setMockColumnsMetasReturnValue(columnMetas);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        realTableMeta = (TableMeta) method.invoke(null, dataSourceProxy, "t1");
        Assertions.assertNotEquals(cacheTableMeta, realTableMeta);
        TableMetaCache.refresh(dataSourceProxy);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        Assertions.assertEquals(cacheTableMeta, realTableMeta);

        //change the column charOctetLength
        columnMetas =
            new Object[][] {
                new Object[] {"", "", "t1", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
                new Object[] {"", "", "t1", "name1", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                new Object[] {"", "", "t1", "name2", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 3, "YES", "NO"},
                new Object[] {"", "", "t1", "name3", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 5, "YES", "NO"},
                new Object[] {"", "", "t1", "id_card", Types.DECIMAL, "DECIMAL", 20, 0, 10, 1, "", "", 0, 0, 64, 4, "NO", "YES"}
            };
        mockDriver.setMockColumnsMetasReturnValue(columnMetas);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        realTableMeta = (TableMeta) method.invoke(null, dataSourceProxy, "t1");
        Assertions.assertNotEquals(cacheTableMeta, realTableMeta);
        TableMetaCache.refresh(dataSourceProxy);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        Assertions.assertEquals(cacheTableMeta, realTableMeta);

        //change the column sqlDataType(unused)

        //change the column sqlDatetimeSub(unused)

        //change the column columnDef
        columnMetas =
            new Object[][] {
                new Object[] {"", "", "t1", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
                new Object[] {"", "", "t1", "name1", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                new Object[] {"", "", "t1", "name2", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 3, "YES", "NO"},
                new Object[] {"", "", "t1", "name3", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 5, "YES", "NO"},
                new Object[] {"", "", "t1", "id_card", Types.DECIMAL, "DECIMAL", 20, 0, 10, 1, "", "001", 0, 0, 64, 4, "NO", "YES"}
            };
        mockDriver.setMockColumnsMetasReturnValue(columnMetas);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        realTableMeta = (TableMeta) method.invoke(null, dataSourceProxy, "t1");
        Assertions.assertNotEquals(cacheTableMeta, realTableMeta);
        TableMetaCache.refresh(dataSourceProxy);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        Assertions.assertEquals(cacheTableMeta, realTableMeta);

        //change the column remarks
        columnMetas =
            new Object[][] {
                new Object[] {"", "", "t1", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
                new Object[] {"", "", "t1", "name1", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                new Object[] {"", "", "t1", "name2", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 3, "YES", "NO"},
                new Object[] {"", "", "t1", "name3", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 5, "YES", "NO"},
                new Object[] {"", "", "t1", "id_card", Types.DECIMAL, "DECIMAL", 20, 0, 10, 1, "ID Card", "001", 0, 0, 64, 4, "NO", "YES"}
            };
        mockDriver.setMockColumnsMetasReturnValue(columnMetas);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        realTableMeta = (TableMeta) method.invoke(null, dataSourceProxy, "t1");
        Assertions.assertNotEquals(cacheTableMeta, realTableMeta);
        TableMetaCache.refresh(dataSourceProxy);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        Assertions.assertEquals(cacheTableMeta, realTableMeta);

        //change the column numPrecRadix
        columnMetas =
            new Object[][] {
                new Object[] {"", "", "t1", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
                new Object[] {"", "", "t1", "name1", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                new Object[] {"", "", "t1", "name2", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 3, "YES", "NO"},
                new Object[] {"", "", "t1", "name3", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 5, "YES", "NO"},
                new Object[] {"", "", "t1", "id_card", Types.DECIMAL, "DECIMAL", 20, 0, 2, 1, "ID Card", "001", 0, 0, 64, 4, "NO", "YES"}
            };
        mockDriver.setMockColumnsMetasReturnValue(columnMetas);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        realTableMeta = (TableMeta) method.invoke(null, dataSourceProxy, "t1");
        Assertions.assertNotEquals(cacheTableMeta, realTableMeta);
        TableMetaCache.refresh(dataSourceProxy);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        Assertions.assertEquals(cacheTableMeta, realTableMeta);

        //change the column date type and date type name
        columnMetas =
            new Object[][] {
                new Object[] {"", "", "t1", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
                new Object[] {"", "", "t1", "name1", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                new Object[] {"", "", "t1", "name2", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 3, "YES", "NO"},
                new Object[] {"", "", "t1", "name3", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 5, "YES", "NO"},
                new Object[] {"", "", "t1", "id_card", Types.VARCHAR, "VARCHAR", 20, 0, 2, 1, "ID Card", "001", 0, 0, 64, 4, "NO", "YES"}
            };
        mockDriver.setMockColumnsMetasReturnValue(columnMetas);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        realTableMeta = (TableMeta) method.invoke(null, dataSourceProxy, "t1");
        Assertions.assertNotEquals(cacheTableMeta, realTableMeta);
        TableMetaCache.refresh(dataSourceProxy);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        Assertions.assertEquals(cacheTableMeta, realTableMeta);

        //change the column name
        columnMetas =
            new Object[][] {
                new Object[] {"", "", "t1", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
                new Object[] {"", "", "t1", "name1", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                new Object[] {"", "", "t1", "name2", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 3, "YES", "NO"},
                new Object[] {"", "", "t1", "name3", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 5, "YES", "NO"},
                new Object[] {"", "", "t1", "id_card_number", Types.VARCHAR, "VARCHAR", 20, 0, 2, 1, "ID Card", "001", 0, 0, 64, 4, "NO", "YES"}
            };
        mockDriver.setMockColumnsMetasReturnValue(columnMetas);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        realTableMeta = (TableMeta) method.invoke(null, dataSourceProxy, "t1");
        Assertions.assertNotEquals(cacheTableMeta, realTableMeta);
        TableMetaCache.refresh(dataSourceProxy);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        Assertions.assertEquals(cacheTableMeta, realTableMeta);

        //change the column tableSchemaName
        columnMetas =
            new Object[][] {
                new Object[] {"", "user", "t1", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
                new Object[] {"", "user", "t1", "name1", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                new Object[] {"", "user", "t1", "name2", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 3, "YES", "NO"},
                new Object[] {"", "user", "t1", "name3", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 5, "YES", "NO"},
                new Object[] {"", "user", "t1", "id_card_number", Types.VARCHAR, "VARCHAR", 20, 0, 2, 1, "ID Card", "001", 0, 0, 64, 4, "NO", "YES"}
            };
        mockDriver.setMockColumnsMetasReturnValue(columnMetas);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        realTableMeta = (TableMeta) method.invoke(null, dataSourceProxy, "t1");
        Assertions.assertNotEquals(cacheTableMeta, realTableMeta);
        TableMetaCache.refresh(dataSourceProxy);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        Assertions.assertEquals(cacheTableMeta, realTableMeta);

        //change the column table cat
        columnMetas =
            new Object[][] {
                new Object[] {"t", "user", "t1", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
                new Object[] {"t", "user", "t1", "name1", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                new Object[] {"t", "user", "t1", "name2", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 3, "YES", "NO"},
                new Object[] {"t", "user", "t1", "name3", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 5, "YES", "NO"},
                new Object[] {"t", "user", "t1", "id_card_number", Types.VARCHAR, "VARCHAR", 20, 0, 2, 1, "ID Card", "001", 0, 0, 64, 4, "NO", "YES"}
            };
        mockDriver.setMockColumnsMetasReturnValue(columnMetas);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        realTableMeta = (TableMeta) method.invoke(null, dataSourceProxy, "t1");
        Assertions.assertNotEquals(cacheTableMeta, realTableMeta);
        TableMetaCache.refresh(dataSourceProxy);
        cacheTableMeta = TableMetaCache.getTableMeta(dataSourceProxy, "t1");
        Assertions.assertEquals(cacheTableMeta, realTableMeta);
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
        Assertions.assertEquals(expected[4], (int)actual.getType());
        Assertions.assertEquals(expected[5], actual.getOrdinalPosition());
        Assertions.assertEquals(expected[6], actual.getAscOrDesc());
        Assertions.assertEquals(expected[7], actual.getCardinality());
    }
}
