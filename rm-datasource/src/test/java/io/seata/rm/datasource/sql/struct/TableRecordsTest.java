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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import io.seata.rm.datasource.exception.TableMetaException;
import io.seata.sqlparser.struct.TableMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.alibaba.druid.mock.MockStatement;
import com.alibaba.druid.mock.MockStatementBase;
import com.alibaba.druid.pool.DruidDataSource;
import com.google.common.collect.Lists;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.mock.MockDriver;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * the table records test
 * @author will
 */
public class TableRecordsTest {

    private static Object[][] columnMetas =
        new Object[][] {
            new Object[] {"", "", "table_records_test", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
            new Object[] {"", "", "table_records_test", "name", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
            new Object[] {"", "", "table_records_test", "information", Types.BLOB, "BLOB", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
            new Object[] {"", "", "table_records_test", "description", Types.CLOB, "CLOB", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
        };

    private static Object[][] columnMetasNewField =
            new Object[][] {
                    new Object[] {"", "", "table_records_test", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
                    new Object[] {"", "", "table_records_test", "name", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                    new Object[] {"", "", "table_records_test", "information", Types.BLOB, "BLOB", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                    new Object[] {"", "", "table_records_test", "description", Types.CLOB, "CLOB", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
                    new Object[] {"", "", "table_records_test", "newf", Types.CLOB, "CLOB", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
            };

    private static Object[][] indexMetas =
        new Object[][] {
            new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},
        };

    private static List<String> returnValueColumnLabels = Lists.newArrayList("id", "name", "information", "description");

    private static List<String> returnValueColumnLabelsNewField = Lists.newArrayList("id", "name", "information", "description","newf");

    private static Object[][] returnValue =
        new Object[][] {
            new Object[] {1, "Tom", "hello", "world"},
            new Object[] {2, "Jack", "hello", "world"},
        };

    private static Object[][] returnValueNewField =
            new Object[][] {
                    new Object[] {1, "Tom", "hello", "world","newf"},
                    new Object[] {2, "Jack", "hello", "world","newf"},
            };

    @BeforeEach
    public void initBeforeEach() {
    }

    @Test
    public void testTableRecords() {

        Assertions.assertThrows(ShouldNeverHappenException.class, () -> {
            TableRecords tableRecords = new TableRecords(new TableMeta());
            tableRecords.setTableMeta(new TableMeta());
        });

        TableRecords tableRecords = new TableRecords(new TableMeta());
        Assertions.assertEquals(0, tableRecords.size());
    }

    @Test
    public void testPkRow() throws SQLException {
        MockDriver mockDriver = new MockDriver(returnValueColumnLabels, returnValue, columnMetas, indexMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);
        MockStatementBase mockStatement = new MockStatement(dataSource.getConnection().getConnection());
        DataSourceProxy proxy = new DataSourceProxy(dataSource);

        TableMeta tableMeta = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.MYSQL).getTableMeta(proxy.getPlainConnection(),
            "table_records_test", proxy.getResourceId());

        ResultSet resultSet = mockDriver.executeQuery(mockStatement, "select * from table_records_test");

        TableRecords tableRecords = TableRecords.buildRecords(tableMeta, resultSet);

        Assertions.assertEquals(returnValue.length, tableRecords.pkRows().size());
    }

    @Test
    public void testBuildRecords() throws SQLException {
        MockDriver mockDriver = new MockDriver(returnValueColumnLabels, returnValue, columnMetas, indexMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);
        MockStatementBase mockStatement = new MockStatement(dataSource.getConnection().getConnection());
        DataSourceProxy proxy = new DataSourceProxy(dataSource);

        TableMeta tableMeta = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.MYSQL).getTableMeta(proxy.getPlainConnection(),
            "table_records_test", proxy.getResourceId());

        ResultSet resultSet = mockDriver.executeQuery(mockStatement, "select * from table_records_test");

        TableRecords tableRecords = TableRecords.buildRecords(tableMeta, resultSet);

        Assertions.assertNotNull(tableRecords);
    }

    @Test
    public void testBuildRecordsNewFeild() throws SQLException {
        MockDriver mockDriver = new MockDriver(returnValueColumnLabels, returnValue, columnMetas, indexMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);
        MockStatementBase mockStatement = new MockStatement(dataSource.getConnection().getConnection());
        DataSourceProxy proxy = new DataSourceProxy(dataSource);

        TableMeta tableMeta = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.MYSQL).getTableMeta(proxy.getPlainConnection(),
                "table_records_test", proxy.getResourceId());


        //  模拟新字段增加
        MockDriver mockDriverNewField = new MockDriver(returnValueColumnLabelsNewField, returnValueNewField, columnMetasNewField, indexMetas);
        ResultSet resultSet = mockDriverNewField.executeQuery(mockStatement, "select * from table_records_test");
        Assertions.assertThrows(TableMetaException.class, () -> TableRecords.buildRecords(tableMeta, resultSet));
    }

    @Test
    public void testEmpty() {
        TableRecords.EmptyTableRecords emptyTableRecords = new TableRecords.EmptyTableRecords();
        Assertions.assertEquals(0, emptyTableRecords.size());

        TableRecords empty = TableRecords.empty(new TableMeta());

        Assertions.assertEquals(0, empty.size());
        Assertions.assertEquals(0, empty.getRows().size());
        Assertions.assertEquals(0, empty.pkRows().size());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> empty.add(new Row()));
        Assertions.assertThrows(UnsupportedOperationException.class, empty::getTableMeta);
    }
}
