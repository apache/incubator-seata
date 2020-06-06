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
package io.seata.rm.lcn;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import com.alibaba.druid.pool.DruidDataSource;
import com.mysql.jdbc.JDBC4MySQLConnection;
import io.seata.core.context.RootContext;
import io.seata.rm.datasource.lcn.ConnectionProxyLcn;
import io.seata.rm.datasource.lcn.DataSourceProxyLcn;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for DataSourceProxyLcn
 *
 * @author funkye
 */
public class DataSourceProxyLcnTest {

    @Test
    public void testGetConnection() throws SQLException {
        // Mock
        Driver driver = Mockito.mock(Driver.class);
        JDBC4MySQLConnection connection = Mockito.mock(JDBC4MySQLConnection.class);
        Mockito.when(connection.getAutoCommit()).thenReturn(true);
        DatabaseMetaData metaData = Mockito.mock(DatabaseMetaData.class);
        Mockito.when(metaData.getURL()).thenReturn("jdbc:mysql:xxx");
        Mockito.when(connection.getMetaData()).thenReturn(metaData);
        Mockito.when(driver.connect(any(), any())).thenReturn(connection);

        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setDriver(driver);
        DataSourceProxyLcn dataSourceProxyLcn = new DataSourceProxyLcn(druidDataSource);
        Connection connection1 = dataSourceProxyLcn.getConnection();

        Assertions.assertFalse(connection1 instanceof ConnectionProxyLcn);
        RootContext.bind("123456");

        Connection connection2 = dataSourceProxyLcn.getConnection();
        Assertions.assertTrue(connection2 instanceof ConnectionProxyLcn);
    }
}
