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

import io.seata.rm.BaseDataSourceResource;

import javax.sql.PooledConnection;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Abstract DataSource proxy for XA mode.
 *
 * @author sharajava
 */
public abstract class AbstractDataSourceProxyXA extends BaseDataSourceResource<ConnectionProxyXA> {

    protected static final String DEFAULT_RESOURCE_GROUP_ID = "DEFAULT_XA";

    /**
     * Get a ConnectionProxyXA instance for finishing XA branch(XA commit/XA rollback)
     * @return ConnectionProxyXA instance
     * @throws SQLException exception
     */
    public ConnectionProxyXA getConnectionForXAFinish(XAXid xaXid) throws SQLException {
        ConnectionProxyXA connectionProxyXA = lookup(xaXid.toString());
        if (connectionProxyXA != null) {
            return connectionProxyXA;
        }
        return (ConnectionProxyXA)getConnectionProxyXA();
    }

    protected abstract Connection getConnectionProxyXA() throws SQLException;

    /**
     * Force close the physical connection kept for XA branch of given XAXid.
     * @param xaXid the given XAXid
     * @throws SQLException exception
     */
    public void forceClosePhysicalConnection(XAXid xaXid) throws SQLException {
        ConnectionProxyXA connectionProxyXA = lookup(xaXid.toString());
        if (connectionProxyXA != null) {
            connectionProxyXA.close();
            Connection physicalConn = connectionProxyXA.getWrappedConnection();
            if (physicalConn instanceof PooledConnection) {
                physicalConn = ((PooledConnection)physicalConn).getConnection();
            }
            // Force close the physical connection
            physicalConn.close();
        }


    }
}
