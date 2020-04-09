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
package io.seata.rm.datasource.xa;

import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.rm.datasource.util.JdbcUtils;
import io.seata.rm.datasource.util.XAUtils;

import javax.sql.DataSource;
import javax.sql.PooledConnection;
import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * DataSource proxy for XA mode.
 *
 * @author sharajava
 */
public class DataSourceProxyXA extends AbstractDataSourceProxyXA {

    public DataSourceProxyXA(DataSource dataSource) {
        this(dataSource, DEFAULT_RESOURCE_GROUP_ID);
    }

    public DataSourceProxyXA(DataSource dataSource, String resourceGroupId) {
        this.dataSource = dataSource;
        this.branchType = BranchType.XA;
        JdbcUtils.initDataSourceResource(this, dataSource, resourceGroupId);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = dataSource.getConnection();
        return getConnectionProxy(connection);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = dataSource.getConnection(username, password);
        return getConnectionProxy(connection);
    }

    protected Connection getConnectionProxy(Connection connection) throws SQLException {
        Connection physicalConn = connection;
        if (connection instanceof PooledConnection) {
            physicalConn = ((PooledConnection)connection).getConnection();
        }
        XAConnection xaConnection = XAUtils.createXAConnection(physicalConn, this);
        ConnectionProxyXA connectionProxyXA = new ConnectionProxyXA(connection, xaConnection, this, RootContext.getXID());
        connectionProxyXA.init();
        return connectionProxyXA;

    }
}
