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

import com.alibaba.druid.mock.MockStatement;
import com.alibaba.druid.mock.MockStatementBase;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.util.JdbcConstants;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.mock.MockDriver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * the table records test
 * @author will
 */
public class TableRecordsTest {

    private static Object[][] columnMetas =
        new Object[][] {
            new Object[] {"", "", "table_records_test", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
            new Object[] {"", "", "table_records_test", "name", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
        };

    private static Object[][] indexMetas =
        new Object[][] {
            new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},
        };

    @BeforeEach
    public void initBeforeEach() {
    }

    @Test
    public void testBuildRecords() throws SQLException {
        MockDriver mockDriver = new MockDriver(columnMetas, indexMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);
        MockStatementBase mockStatement = new MockStatement(dataSource.getConnection().getConnection());
        DataSourceProxy proxy = new DataSourceProxy(dataSource);

        TableMeta tableMeta = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.MYSQL).getTableMeta(proxy, "table_records_test");

        ResultSet resultSet = mockDriver.executeQuery(mockStatement, "select * from table_records_test");

        TableRecords tableRecords = TableRecords.buildRecords(tableMeta, resultSet);

        Assertions.assertNotNull(tableRecords);
    }
}
