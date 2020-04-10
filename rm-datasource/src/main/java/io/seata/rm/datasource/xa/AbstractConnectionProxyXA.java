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
import io.seata.rm.BaseDataSourceResource;

import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * The type Abstract connection proxy on XA mode.
 *
 * @author sharajava
 */
public abstract class AbstractConnectionProxyXA implements Connection {

    public static final String SQLSTATE_XA_NOT_END = "SQLSTATE_XA_NOT_END";

    protected Connection originalConnection;

    protected XAConnection xaConnection;

    protected XAResource xaResource;

    protected BaseDataSourceResource resource;

    protected String xid;

    public AbstractConnectionProxyXA(Connection originalConnection, XAConnection xaConnection, BaseDataSourceResource resource, String xid) {
        this.originalConnection = originalConnection;
        this.xaConnection = xaConnection;
        this.resource = resource;
        this.xid = xid;
    }

    public XAConnection getWrappedXAConnection() {
        return xaConnection;
    }

    public Connection getWrappedConnection() {
        return originalConnection;
    }

    @Override
    public Statement createStatement() throws SQLException {
        Statement targetStatement = originalConnection.createStatement();
        return new StatementProxyXA(this, targetStatement);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        PreparedStatement targetStatement = originalConnection.prepareStatement(sql);
        return new PreparedStatementProxyXA(this, targetStatement);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        RootContext.assertNotInGlobalTransaction();
        return originalConnection.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return originalConnection.nativeSQL(sql);
    }

    @Override
    public boolean isClosed() throws SQLException {
        return originalConnection.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return originalConnection.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        originalConnection.setReadOnly(readOnly);

    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return originalConnection.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        originalConnection.setCatalog(catalog);

    }

    @Override
    public String getCatalog() throws SQLException {
        return originalConnection.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        originalConnection.setTransactionIsolation(level);

    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return originalConnection.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return originalConnection.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        originalConnection.clearWarnings();

    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return originalConnection.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        originalConnection.setTypeMap(map);

    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        originalConnection.setHoldability(holdability);

    }

    @Override
    public int getHoldability() throws SQLException {
        return originalConnection.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return originalConnection.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return originalConnection.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        originalConnection.rollback(savepoint);

    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        originalConnection.releaseSavepoint(savepoint);

    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        Statement statement = originalConnection.createStatement(resultSetType, resultSetConcurrency);
        return new StatementProxyXA(this, statement);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
        throws SQLException {
        PreparedStatement preparedStatement = originalConnection.prepareStatement(sql, resultSetType,
            resultSetConcurrency);
        return new PreparedStatementProxyXA(this, preparedStatement);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        RootContext.assertNotInGlobalTransaction();
        return originalConnection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        Statement statement = originalConnection.createStatement(resultSetType, resultSetConcurrency,
                resultSetHoldability);
        return new StatementProxyXA(this, statement);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
                                              int resultSetHoldability) throws SQLException {
        PreparedStatement preparedStatement = originalConnection.prepareStatement(sql, resultSetType,
                resultSetConcurrency, resultSetHoldability);
        return new PreparedStatementProxyXA(this, preparedStatement);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
                                         int resultSetHoldability) throws SQLException {
        RootContext.assertNotInGlobalTransaction();
        return originalConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        PreparedStatement preparedStatement = originalConnection.prepareStatement(sql, autoGeneratedKeys);
        return new PreparedStatementProxyXA(this, preparedStatement);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        PreparedStatement preparedStatement = originalConnection.prepareStatement(sql, columnIndexes);
        return new PreparedStatementProxyXA(this, preparedStatement);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        PreparedStatement preparedStatement = originalConnection.prepareStatement(sql, columnNames);
        return new PreparedStatementProxyXA(this, preparedStatement);
    }

    @Override
    public Clob createClob() throws SQLException {
        return originalConnection.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return originalConnection.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return originalConnection.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return originalConnection.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return originalConnection.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        originalConnection.setClientInfo(name, value);

    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        originalConnection.setClientInfo(properties);

    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return originalConnection.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return originalConnection.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return originalConnection.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return originalConnection.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        originalConnection.setSchema(schema);

    }

    @Override
    public String getSchema() throws SQLException {
        return originalConnection.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        originalConnection.abort(executor);

    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        originalConnection.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return originalConnection.getNetworkTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return originalConnection.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return originalConnection.isWrapperFor(iface);
    }
}
