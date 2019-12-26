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
package io.seata.rm.datasource.exec;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import com.alibaba.druid.mock.MockStatement;
import com.alibaba.druid.mock.MockStatementBase;
import com.alibaba.druid.pool.DruidDataSource;
import com.google.common.collect.Lists;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.mock.MockDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author will
 */
public class PlainExecutorTest {

    private PlainExecutor plainExecutor;

    private StatementProxy statementProxy;

    @BeforeEach
    public void init() throws SQLException {
        List<String> returnValueColumnLabels = Lists.newArrayList("id", "name");
        Object[][] returnValue = new Object[][] {
            new Object[] {1, "Tom"},
            new Object[] {2, "Jack"},
        };
        Object[][] columnMetas = new Object[][] {
            new Object[] {"", "", "table_plain_executor_test", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
            new Object[] {"", "", "table_plain_executor_test", "name", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
        };
        Object[][] indexMetas = new Object[][] {
            new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},
        };

        MockDriver mockDriver = new MockDriver(returnValueColumnLabels, returnValue, columnMetas, indexMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy dataSourceProxy = new DataSourceProxy(dataSource);
        ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, dataSource.getConnection().getConnection());
        MockStatementBase mockStatement = new MockStatement(dataSource.getConnection().getConnection());
        statementProxy = new StatementProxy(connectionProxy, mockStatement);

        plainExecutor = new PlainExecutor(statementProxy, (statement, args) -> null);
    }

    @Test
    public void testExecute() throws Throwable {
        plainExecutor.execute(null);
    }

}
