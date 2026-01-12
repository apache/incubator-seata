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
package org.apache.seata.rm.datasource.xa;

import org.apache.seata.common.DefaultValues;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.DBType;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.protocol.Version;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.rm.datasource.SeataDataSourceProxy;
import org.apache.seata.rm.datasource.combine.CombineConnectionHolder;
import org.apache.seata.rm.datasource.util.JdbcUtils;
import org.apache.seata.rm.datasource.util.XAUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import java.sql.*;
import java.util.AbstractMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.seata.common.ConfigurationKeys.SONATA_ENABLE_GLOBAL_SERIALIZABILITY;

/**
 * DataSource proxy for XA mode.
 *
 */
public class DataSourceProxyXA extends AbstractDataSourceProxyXA {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceProxyXA.class);

    private static final boolean SHOULD_ENABLE_SONATA = ConfigurationFactory.getInstance()
            .getBoolean(
                    SONATA_ENABLE_GLOBAL_SERIALIZABILITY, DefaultValues.DEFAULT_SONATA_ENABLE_GLOBAL_SERIALIZABILITY);

    protected final boolean sonataS2plShimEnabled;
    protected final boolean sonataSsiShimEnabled;
    protected final boolean sonataShimEnabled;

    // For both S2PL and SSI DBs
    protected final ConcurrentSkipListSet<Integer> ACTIVE_DUMMY_KEYS = new ConcurrentSkipListSet<>();
    protected final ConcurrentMap<XAXid, Integer> XID_TO_DUMMY_KEY = new ConcurrentHashMap<>();

    // For SSI DBs only
    protected final ConcurrentMap<Integer, AtomicInteger> HELPER_ID_REF_COUNT = new ConcurrentHashMap<>();
    protected final ConcurrentLinkedQueue<AbstractMap.SimpleEntry<Integer, Integer>>
            RESERVED_DUMMY_KEYS_AND_HELPER_IDS = new ConcurrentLinkedQueue<>();
    protected final ConcurrentMap<XAXid, Integer> XID_TO_HELPER_ID = new ConcurrentHashMap<>();

    public DataSourceProxyXA(DataSource dataSource) {
        this(dataSource, DEFAULT_RESOURCE_GROUP_ID);
    }

    public DataSourceProxyXA(DataSource dataSource, String resourceGroupId) {
        if (dataSource instanceof SeataDataSourceProxy) {
            LOGGER.info(
                    "Unwrap the data source, because the type is: {}",
                    dataSource.getClass().getName());
            dataSource = ((SeataDataSourceProxy) dataSource).getTargetDataSource();
        }
        this.dataSource = dataSource;
        this.branchType = BranchType.XA;
        JdbcUtils.initDataSourceResource(this, dataSource, resourceGroupId);
        if (DBType.MYSQL.name().equalsIgnoreCase(dbType)) {
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement preparedStatement = connection.prepareStatement("SELECT VERSION()");
                    ResultSet versionResult = preparedStatement.executeQuery()) {
                if (versionResult.next()) {
                    long currentVersion = Version.convertVersion(versionResult.getString("VERSION()"));
                    long version = Version.convertVersion("8.0.29");
                    if (currentVersion < version) {
                        setShouldBeHeld(true);
                    }
                }
            } catch (Exception e) {
                setShouldBeHeld(true);
                LOGGER.info("get mysql version fail error: {}", e.getMessage());
            }
        } else if (DBType.MARIADB.name().equalsIgnoreCase(dbType)) {
            setShouldBeHeld(true);
        } else if (DBType.OSCAR.name().equalsIgnoreCase(dbType)) {
            setShouldBeHeld(true);
        }
        Optional.ofNullable(DefaultResourceManager.get().getResourceManager(BranchType.XA))
                .ifPresent(resourceManager -> {
                    if (resourceManager instanceof ResourceManagerXA) {
                        ((ResourceManagerXA) resourceManager).initXaTwoPhaseTimeoutChecker();
                    }
                });
        // Set the default branch type to 'XA' in the RootContext.
        RootContext.setDefaultBranchType(this.getBranchType());

        sonataS2plShimEnabled = SHOULD_ENABLE_SONATA && DBType.MYSQL.name().equalsIgnoreCase(dbType);
        sonataSsiShimEnabled = SHOULD_ENABLE_SONATA && DBType.POSTGRESQL.name().equalsIgnoreCase(dbType);
        sonataShimEnabled = sonataS2plShimEnabled || sonataSsiShimEnabled;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (RootContext.inGlobalTransaction() && RootContext.inCombineTransaction()) {
            ConnectionProxyXA connectionProxyXA = CombineConnectionHolder.get(this.dataSource);
            if (connectionProxyXA != null && !connectionProxyXA.isClosed()) {
                return connectionProxyXA;
            }
        }
        Connection connection = dataSource.getConnection();
        return getConnectionProxy(connection);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        if (RootContext.inGlobalTransaction() && RootContext.inCombineTransaction()) {
            ConnectionProxyXA connectionProxyXA = CombineConnectionHolder.get(this.dataSource);
            if (connectionProxyXA != null && !connectionProxyXA.isClosed()) {
                return connectionProxyXA;
            }
        }
        Connection connection = dataSource.getConnection(username, password);
        return getConnectionProxy(connection);
    }

    protected Connection getConnectionProxy(Connection connection) throws SQLException {
        if (!RootContext.inGlobalTransaction()) {
            return connection;
        }
        ConnectionProxyXA connectionProxyXA = (ConnectionProxyXA) getConnectionProxyXA(connection);
        if (RootContext.inCombineTransaction()) {
            CombineConnectionHolder.putConnection(this.dataSource, connectionProxyXA);
        }
        return connectionProxyXA;
    }

    @Override
    protected Connection getConnectionProxyXA() throws SQLException {
        Connection connection = dataSource.getConnection();
        return getConnectionProxyXA(connection);
    }

    private Connection getConnectionProxyXA(Connection connection) throws SQLException {
        Connection physicalConn = connection.unwrap(Connection.class);
        XAConnection xaConnection = XAUtils.createXAConnection(physicalConn, this);
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(connection, xaConnection, this, RootContext.getXID());
        connectionProxyXA.init();
        return connectionProxyXA;
    }

    // Should be auto-executed when the datasource is closed. Now for simplicity we let the application manually
    // release the pending helpers.
    public void releaseAllHelpers() throws SQLException {
        if (!sonataSsiShimEnabled) {
            throw new RuntimeException("Sonata SSI helper transactions are not allowed for the datasource");
        }

        try (Connection conn = dataSource.getConnection()) {
            for (int helperTxnId : HELPER_ID_REF_COUNT.keySet()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("rollback prepared '" + helperTxnId + "'");
                }
            }
        }
    }

    protected Connection getSsiHelperConnection() throws SQLException {
        if (!sonataSsiShimEnabled) {
            throw new RuntimeException("Sonata SSI helper transactions are not allowed for the datasource");
        }

        return dataSource.getConnection();
    }
}
