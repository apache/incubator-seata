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

import com.alibaba.druid.mock.MockStatement;
import com.alibaba.druid.mock.MockStatementBase;
import com.alibaba.druid.pool.DruidDataSource;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Lists;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.DataSourceProxyTest;
import org.apache.seata.rm.datasource.exception.TableMetaException;
import org.apache.seata.rm.datasource.mock.MockDriver;
import org.apache.seata.sqlparser.struct.ColumnMeta;
import org.apache.seata.sqlparser.struct.IndexMeta;
import org.apache.seata.sqlparser.struct.IndexType;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * the table records test
 */
public class TableRecordsTest {

    private static Object[][] columnMetas = new Object[][] {
        new Object[] {
            "", "", "table_records_test", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"
        },
        new Object[] {
            "",
            "",
            "table_records_test",
            "name",
            Types.VARCHAR,
            "VARCHAR",
            64,
            0,
            10,
            0,
            "",
            "",
            0,
            0,
            64,
            2,
            "YES",
            "NO"
        },
        new Object[] {
            "",
            "",
            "table_records_test",
            "information",
            Types.BLOB,
            "BLOB",
            64,
            0,
            10,
            0,
            "",
            "",
            0,
            0,
            64,
            2,
            "YES",
            "NO"
        },
        new Object[] {
            "",
            "",
            "table_records_test",
            "description",
            Types.CLOB,
            "CLOB",
            64,
            0,
            10,
            0,
            "",
            "",
            0,
            0,
            64,
            2,
            "YES",
            "NO"
        },
    };

    private static Object[][] columnMetasNewField = new Object[][] {
        new Object[] {
            "", "", "table_records_test", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"
        },
        new Object[] {
            "",
            "",
            "table_records_test",
            "name",
            Types.VARCHAR,
            "VARCHAR",
            64,
            0,
            10,
            0,
            "",
            "",
            0,
            0,
            64,
            2,
            "YES",
            "NO"
        },
        new Object[] {
            "",
            "",
            "table_records_test",
            "information",
            Types.BLOB,
            "BLOB",
            64,
            0,
            10,
            0,
            "",
            "",
            0,
            0,
            64,
            2,
            "YES",
            "NO"
        },
        new Object[] {
            "",
            "",
            "table_records_test",
            "description",
            Types.CLOB,
            "CLOB",
            64,
            0,
            10,
            0,
            "",
            "",
            0,
            0,
            64,
            2,
            "YES",
            "NO"
        },
        new Object[] {
            "", "", "table_records_test", "newf", Types.CLOB, "CLOB", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"
        },
    };

    private static Object[][] indexMetas = new Object[][] {
        new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},
    };

    private static List<String> returnValueColumnLabels =
            Lists.newArrayList("id", "name", "information", "description");

    private static List<String> returnValueColumnLabelsNewField =
            Lists.newArrayList("id", "name", "information", "description", "newf");

    private static Object[][] returnValue = new Object[][] {
        new Object[] {1, "Tom", "hello", "world"},
        new Object[] {2, "Jack", "hello", "world"},
    };

    private static Object[][] returnValueNewField = new Object[][] {
        new Object[] {1, "Tom", "hello", "world", "newf"},
        new Object[] {2, "Jack", "hello", "world", "newf"},
    };

    @BeforeEach
    public void initBeforeEach() {}

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

        MockStatementBase mockStatement = new MockStatement(getPhysicsConnection(dataSource));
        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);

        TableMeta tableMeta = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.MYSQL)
                .getTableMeta(proxy.getPlainConnection(), "table_records_test", proxy.getResourceId());

        ResultSet resultSet = mockDriver.executeQuery(mockStatement, "select * from table_records_test");

        TableRecords tableRecords = TableRecords.buildRecords(tableMeta, resultSet);

        Assertions.assertEquals(returnValue.length, tableRecords.pkRows().size());
    }

    private Connection getPhysicsConnection(DruidDataSource dataSource) throws SQLException {
        Connection connection = dataSource.getConnection().getConnection();
        return connection.unwrap(Connection.class);
    }

    @Test
    public void testBuildRecords() throws SQLException {
        MockDriver mockDriver = new MockDriver(returnValueColumnLabels, returnValue, columnMetas, indexMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);
        MockStatementBase mockStatement = new MockStatement(getPhysicsConnection(dataSource));
        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);

        TableMeta tableMeta = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.MYSQL)
                .getTableMeta(proxy.getPlainConnection(), "table_records_test", proxy.getResourceId());

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
        MockStatementBase mockStatement = new MockStatement(getPhysicsConnection(dataSource));
        DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);

        TableMeta tableMeta = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.MYSQL)
                .getTableMeta(proxy.getPlainConnection(), "table_records_test", proxy.getResourceId());

        //  模拟新字段增加
        MockDriver mockDriverNewField =
                new MockDriver(returnValueColumnLabelsNewField, returnValueNewField, columnMetasNewField, indexMetas);
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

    private static Object[][] columnMetasOffsetDateTime = new Object[][] {
        new Object[] {
            "", "", "table_records_test", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"
        },
        new Object[] {
            "",
            "",
            "table_records_test",
            "time_col",
            Types.TIMESTAMP_WITH_TIMEZONE,
            "TIMESTAMP_WITH_TIMEZONE",
            64,
            0,
            10,
            0,
            "",
            "",
            0,
            0,
            64,
            2,
            "YES",
            "NO"
        },
    };

    private static List<String> returnValueColumnLabelsOffsetDateTime = Lists.newArrayList("id", "time_col");

    private static Object[][] returnValueOffsetDateTime = new Object[][] {
        new Object[] {1, OffsetDateTime.of(2025, 1, 15, 10, 30, 45, 0, ZoneOffset.UTC)},
    };

    @Test
    public void testBuildRecordsWithOffsetDateTime() throws SQLException, IOException {
        MockDriver mockDriver = new MockDriver(
                returnValueColumnLabelsOffsetDateTime,
                returnValueOffsetDateTime,
                columnMetasOffsetDateTime,
                indexMetas);

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:offset");
        dataSource.setDriver(mockDriver);

        try (MockStatementBase mockStatement = new MockStatement(getPhysicsConnection(dataSource))) {
            DataSourceProxy proxy = DataSourceProxyTest.getDataSourceProxy(dataSource);
            TableMetaCacheFactory.getTableMetaCache(JdbcConstants.POSTGRESQL)
                    .refresh(proxy.getPlainConnection(), proxy.getResourceId());
            TableMeta tableMeta = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.POSTGRESQL)
                    .getTableMeta(proxy.getPlainConnection(), "table_records_test", proxy.getResourceId());
            ColumnMeta idColumnMeta = new ColumnMeta();
            idColumnMeta.setColumnName("id");
            idColumnMeta.setDataType(java.sql.Types.INTEGER);
            idColumnMeta.setTableName("table_records_test");
            idColumnMeta.setIsAutoincrement("NO");
            idColumnMeta.setIsNullAble("NO");

            IndexMeta primaryIndex = new IndexMeta();
            primaryIndex.setIndexName("PRIMARY");
            primaryIndex.setIndextype(IndexType.PRIMARY);
            primaryIndex.getValues().add(idColumnMeta);

            tableMeta.getAllIndexes().put("PRIMARY", primaryIndex);
            if (tableMeta.getColumnMeta("id") == null) {
                tableMeta.getAllColumns().put("id", idColumnMeta);
            }
            ResultSet originalResultSet = mockDriver.executeQuery(mockStatement, "select * from table_records_test");
            ResultSet proxyResultSet = (ResultSet) java.lang.reflect.Proxy.newProxyInstance(
                    TableRecordsTest.class.getClassLoader(), new Class[] {ResultSet.class}, (p, method, args) -> {
                        if ("getObject".equals(method.getName())
                                && args.length == 2
                                && args[1] == OffsetDateTime.class) {
                            return originalResultSet.getObject((Integer) args[0]);
                        }
                        try {
                            return method.invoke(originalResultSet, args);
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            throw e.getTargetException();
                        }
                    });
            TableRecords tableRecords = TableRecords.buildRecords(tableMeta, proxyResultSet);
            Assertions.assertNotNull(tableRecords);
            Assertions.assertEquals(1, tableRecords.size());
            Row row = tableRecords.getRows().get(0);
            Field timeField = row.getFields().stream()
                    .filter(f -> "time_col".equalsIgnoreCase(f.getName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("time_col not found"));
            Assertions.assertEquals(Types.TIMESTAMP_WITH_TIMEZONE, timeField.getType());
            Assertions.assertTrue(timeField.getValue() instanceof OffsetDateTime);

            OffsetDateTime originalTime = (OffsetDateTime) timeField.getValue();

            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .disableDefaultTyping();

            byte[] encodedBytes = mapper.writeValueAsBytes(originalTime);
            OffsetDateTime deserializedTime = mapper.readValue(encodedBytes, OffsetDateTime.class);
            Assertions.assertEquals(originalTime, deserializedTime);
        } finally {
            dataSource.close();
        }
    }
}
