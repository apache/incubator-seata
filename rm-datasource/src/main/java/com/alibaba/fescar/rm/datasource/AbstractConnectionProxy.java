/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.rm.datasource;

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

import com.alibaba.fescar.core.context.RootContext;

public abstract class AbstractConnectionProxy implements Connection {

    protected DataSourceProxy dataSourceProxy;

    protected Connection targetConnection;

    protected String dbType;

    public AbstractConnectionProxy(DataSourceProxy dataSourceProxy, Connection targetConnection, String dbType) {
        this.dataSourceProxy = dataSourceProxy;
        this.targetConnection = targetConnection;
        this.dbType = dbType;
    }

    public DataSourceProxy getDataSourceProxy() {
        return dataSourceProxy;
    }

    public Connection getTargetConnection() {
        return targetConnection;
    }

    public String getDbType() {
        return dbType;
    }

    @Override
    public Statement createStatement() throws SQLException {
        Statement targetStatement = getTargetConnection().createStatement();
        return new StatementProxy(this, targetStatement);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        PreparedStatement targetPreparedStatement = getTargetConnection().prepareStatement(sql);
        return new PreparedStatementProxy(this, targetPreparedStatement, sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        RootContext.assertNotInGlobalTransaction();
        return targetConnection.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return targetConnection.nativeSQL(sql);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return targetConnection.getAutoCommit();
    }

    @Override
    public void close() throws SQLException {
        targetConnection.close();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return targetConnection.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return targetConnection.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        targetConnection.setReadOnly(readOnly);

    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return targetConnection.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        targetConnection.setCatalog(catalog);

    }

    @Override
    public String getCatalog() throws SQLException {
        return targetConnection.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        targetConnection.setTransactionIsolation(level);

    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return targetConnection.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return targetConnection.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        targetConnection.clearWarnings();

    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        Statement statement = targetConnection.createStatement(resultSetType, resultSetConcurrency);
        return new StatementProxy<Statement>(this, statement);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        PreparedStatement preparedStatement = targetConnection.prepareStatement(sql, resultSetType, resultSetConcurrency);
        return new PreparedStatementProxy(this, preparedStatement, sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        RootContext.assertNotInGlobalTransaction();
        return targetConnection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return targetConnection.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        targetConnection.setTypeMap(map);

    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        targetConnection.setHoldability(holdability);

    }

    @Override
    public int getHoldability() throws SQLException {
        return targetConnection.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return targetConnection.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return targetConnection.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        targetConnection.rollback(savepoint);

    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        targetConnection.releaseSavepoint(savepoint);

    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        Statement statement = targetConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        return new StatementProxy<Statement>(this, statement);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        PreparedStatement preparedStatement = targetConnection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        return new PreparedStatementProxy(this, preparedStatement, sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        RootContext.assertNotInGlobalTransaction();
        return targetConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        PreparedStatement preparedStatement = targetConnection.prepareStatement(sql, autoGeneratedKeys);
        return new PreparedStatementProxy(this, preparedStatement, sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        PreparedStatement preparedStatement = targetConnection.prepareStatement(sql, columnIndexes);
        return new PreparedStatementProxy(this, preparedStatement, sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        PreparedStatement preparedStatement = targetConnection.prepareStatement(sql, columnNames);
        return new PreparedStatementProxy(this, preparedStatement, sql);
    }

    @Override
    public Clob createClob() throws SQLException {
        return targetConnection.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return targetConnection.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return targetConnection.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return targetConnection.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return targetConnection.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        targetConnection.setClientInfo(name, value);

    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        targetConnection.setClientInfo(properties);

    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return targetConnection.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return targetConnection.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return targetConnection.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return targetConnection.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        targetConnection.setSchema(schema);

    }

    @Override
    public String getSchema() throws SQLException {
        return targetConnection.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        targetConnection.abort(executor);

    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        targetConnection.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return targetConnection.getNetworkTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return targetConnection.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return targetConnection.isWrapperFor(iface);
    }
}
