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

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import javax.sql.XAConnection;

import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.rm.datasource.SeataDataSourceProxy;
import io.seata.rm.datasource.util.JdbcUtils;
import io.seata.rm.datasource.util.XAUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DataSource proxy for XA mode.
 *
 * @author sharajava
 */
public class DataSourceProxyXA extends AbstractDataSourceProxyXA {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceProxyXA.class);

    public DataSourceProxyXA(DataSource dataSource) {
        this(dataSource, DEFAULT_RESOURCE_GROUP_ID);
    }

    public DataSourceProxyXA(DataSource dataSource, String resourceGroupId) {
        if (dataSource instanceof SeataDataSourceProxy) {
            LOGGER.info("Unwrap the data source, because the type is: {}", dataSource.getClass().getName());
            dataSource = ((SeataDataSourceProxy) dataSource).getTargetDataSource();
        }
        this.dataSource = dataSource;
        this.branchType = BranchType.XA;
        JdbcUtils.initDataSourceResource(this, dataSource, resourceGroupId);

        //Set the default branch type to 'XA' in the RootContext.
        RootContext.setDefaultBranchType(this.getBranchType());
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
        if (!RootContext.inGlobalTransaction()) {
            return connection;
        }
        return getConnectionProxyXA(connection);
    }

    @Override
    protected Connection getConnectionProxyXA() throws SQLException {
        Connection connection = dataSource.getConnection();
        return getConnectionProxyXA(connection);
    }

    private Connection getConnectionProxyXA(Connection connection) throws SQLException {
        Connection physicalConn = connection.unwrap(Connection.class);
        XAConnection xaConnection = XAUtils.createXAConnection(physicalConn, this);
        ConnectionProxyXA connectionProxyXA = new ConnectionProxyXA(connection, xaConnection, this, RootContext.getXID());
        connectionProxyXA.init();
        return connectionProxyXA;
    }

}
