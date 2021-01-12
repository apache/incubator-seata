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

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * DataSource proxy to wrap a XADataSource.
 *
 * @author sharajava
 */
public class DataSourceProxyXANative extends AbstractDataSourceProxyXA {

    private XADataSource xaDataSource;

    public DataSourceProxyXANative(XADataSource dataSource) {
        this(dataSource, DEFAULT_RESOURCE_GROUP_ID);
    }

    public DataSourceProxyXANative(XADataSource dataSource, String resourceGroupId) {
        if (dataSource instanceof DataSource) {
            this.dataSource = (DataSource)dataSource;
        }
        this.xaDataSource = dataSource;
        this.branchType = BranchType.XA;
        JdbcUtils.initXADataSourceResource(this, dataSource, resourceGroupId);
    }

    @Override
    public Connection getConnection() throws SQLException {
        XAConnection xaConnection = xaDataSource.getXAConnection();
        return getConnectionProxy(xaConnection);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        XAConnection xaConnection = xaDataSource.getXAConnection(username, password);
        return getConnectionProxy(xaConnection);
    }

    protected Connection getConnectionProxy(XAConnection xaConnection) throws SQLException {
        Connection connection = xaConnection.getConnection();
        ConnectionProxyXA connectionProxyXA = new ConnectionProxyXA(connection, xaConnection, this, RootContext.getXID());
        connectionProxyXA.init();
        return connectionProxyXA;

    }

    @Override
    protected Connection getConnectionProxyXA() throws SQLException {
        return getConnection();
    }
}
