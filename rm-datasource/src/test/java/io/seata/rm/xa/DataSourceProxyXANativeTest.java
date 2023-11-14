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
package io.seata.rm.xa;

import io.seata.rm.datasource.xa.ConnectionProxyXA;
import io.seata.rm.datasource.xa.DataSourceProxyXANative;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Tests for DataSourceProxyXANative
 *
 * @author sharajava
 */
public class DataSourceProxyXANativeTest {

    @Test
    public void testGetConnection() throws SQLException {
        // Mock
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit()).thenReturn(true);
        DatabaseMetaData metaData = Mockito.mock(DatabaseMetaData.class);
        Mockito.when(metaData.getURL()).thenReturn("jdbc:mysql:xxx");
        Mockito.when(connection.getMetaData()).thenReturn(metaData);
        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        Mockito.when(xaConnection.getConnection()).thenReturn(connection);
        XADataSource xaDataSource = Mockito.mock(XADataSource.class);
        Mockito.when(xaDataSource.getXAConnection()).thenReturn(xaConnection);

        DataSourceProxyXANative dataSourceProxyXANative = new DataSourceProxyXANative(xaDataSource);
        Connection connFromDataSourceProxyXANative = dataSourceProxyXANative.getConnection();

        Assertions.assertTrue(connFromDataSourceProxyXANative instanceof ConnectionProxyXA);
        XAConnection xaConnectionFromProxy = ((ConnectionProxyXA)connFromDataSourceProxyXANative).getWrappedXAConnection();
        Assertions.assertSame(xaConnection, xaConnectionFromProxy);
        Connection connectionFromProxy = ((ConnectionProxyXA)connFromDataSourceProxyXANative).getWrappedConnection();
        Assertions.assertSame(connection, connectionFromProxy);

    }
}
