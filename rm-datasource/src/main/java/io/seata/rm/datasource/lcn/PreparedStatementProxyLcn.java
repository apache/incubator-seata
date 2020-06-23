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
package io.seata.rm.datasource.lcn;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import io.seata.common.util.StringUtils;
import io.seata.sqlparser.struct.Null;

/**
 * @author funkye
 */
public class PreparedStatementProxyLcn implements PreparedStatement {

    /**
     * The Parameters.
     */
    protected ArrayList<Object>[] parameters;

    protected PreparedStatement targetStatement;

    protected String targetSql;

    public PreparedStatementProxyLcn(PreparedStatement targetStatement, String targetSql) throws SQLException {
        this.targetStatement = targetStatement;
        this.targetSql = targetSql;
        initParameterHolder();
    }

    private void initParameterHolder() throws SQLException {
        int paramCount = targetStatement.getParameterMetaData().getParameterCount();
        this.parameters = new ArrayList[paramCount];
        for (int i = 0; i < paramCount; i++) {
            parameters[i] = new ArrayList<>();
        }
    }

    /**
     * Gets params by index.
     *
     * @param index
     *            the index
     * @return the params by index
     */
    public List<Object> getParamsByIndex(int index) {
        return parameters[index];
    }

    /**
     * Sets param by index.
     *
     * @param index
     *            the index
     * @param x
     *            the x
     */
    protected void setParamByIndex(int index, Object x) {
        parameters[--index].add(x);
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        return targetStatement.executeQuery();
    }

    @Override
    public int executeUpdate() throws SQLException {
        return targetStatement.executeUpdate();
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        setParamByIndex(parameterIndex, Null.get());
        targetStatement.setNull(parameterIndex, sqlType);
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setBoolean(parameterIndex, x);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setByte(parameterIndex, x);

    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setShort(parameterIndex, x);

    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setInt(parameterIndex, x);

    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setLong(parameterIndex, x);

    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setFloat(parameterIndex, x);

    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setDouble(parameterIndex, x);

    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setBigDecimal(parameterIndex, x);

    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setString(parameterIndex, x);

    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setBytes(parameterIndex, x);

    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setDate(parameterIndex, x);

    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setTime(parameterIndex, x);

    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setTimestamp(parameterIndex, x);

    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setAsciiStream(parameterIndex, x, length);

    }

    @Deprecated
    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setUnicodeStream(parameterIndex, x, length);

    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setBinaryStream(parameterIndex, x, length);

    }

    @Override
    public void clearParameters() throws SQLException {
        initParameterHolder();
        targetStatement.clearParameters();

    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setObject(parameterIndex, x, targetSqlType);
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setObject(parameterIndex, x);
    }

    @Override
    public boolean execute() throws SQLException {
        return targetStatement.execute();
    }

    @Override
    public void addBatch() throws SQLException {
        targetStatement.addBatch();
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        setParamByIndex(parameterIndex, reader);
        targetStatement.setCharacterStream(parameterIndex, reader, length);

    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setRef(parameterIndex, x);

    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setBlob(parameterIndex, x);

    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setClob(parameterIndex, x);

    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setArray(parameterIndex, x);

    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return targetStatement.getMetaData();
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setDate(parameterIndex, x, cal);

    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setTime(parameterIndex, x, cal);

    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setTimestamp(parameterIndex, x, cal);

    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        setParamByIndex(parameterIndex, Null.get());
        targetStatement.setNull(parameterIndex, sqlType, typeName);

    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setURL(parameterIndex, x);

    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return targetStatement.getParameterMetaData();
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setRowId(parameterIndex, x);

    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        setParamByIndex(parameterIndex, value);
        targetStatement.setNString(parameterIndex, value);

    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        setParamByIndex(parameterIndex, value);
        targetStatement.setNCharacterStream(parameterIndex, value, length);

    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        setParamByIndex(parameterIndex, value);
        targetStatement.setNClob(parameterIndex, value);

    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        setParamByIndex(parameterIndex, reader);
        targetStatement.setClob(parameterIndex, reader, length);

    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        setParamByIndex(parameterIndex, inputStream);
        targetStatement.setBlob(parameterIndex, inputStream, length);

    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        setParamByIndex(parameterIndex, reader);
        targetStatement.setNClob(parameterIndex, reader, length);

    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        setParamByIndex(parameterIndex, xmlObject);
        targetStatement.setSQLXML(parameterIndex, xmlObject);

    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setObject(parameterIndex, x, targetSqlType, scaleOrLength);

    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setAsciiStream(parameterIndex, x, length);

    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setBinaryStream(parameterIndex, x, length);

    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        setParamByIndex(parameterIndex, reader);
        targetStatement.setCharacterStream(parameterIndex, reader, length);

    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setAsciiStream(parameterIndex, x);

    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        setParamByIndex(parameterIndex, x);
        targetStatement.setBinaryStream(parameterIndex, x);

    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        setParamByIndex(parameterIndex, reader);
        targetStatement.setCharacterStream(parameterIndex, reader);

    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        setParamByIndex(parameterIndex, value);
        targetStatement.setNCharacterStream(parameterIndex, value);

    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        setParamByIndex(parameterIndex, reader);
        targetStatement.setClob(parameterIndex, reader);

    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        setParamByIndex(parameterIndex, inputStream);
        targetStatement.setBlob(parameterIndex, inputStream);

    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        setParamByIndex(parameterIndex, reader);
        targetStatement.setNClob(parameterIndex, reader);
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        return targetStatement.executeQuery(sql);
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        return targetStatement.executeUpdate(sql);
    }

    @Override
    public void close() throws SQLException {
        targetStatement.close();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return targetStatement.getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        targetStatement.setMaxFieldSize(max);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return targetStatement.getMaxRows();
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        targetStatement.setMaxRows(max);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        targetStatement.setEscapeProcessing(enable);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return targetStatement.getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        targetStatement.setQueryTimeout(seconds);
    }

    @Override
    public void cancel() throws SQLException {
        targetStatement.cancel();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return targetStatement.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        targetStatement.clearWarnings();
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        targetStatement.setCursorName(name);
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        return targetStatement.execute();
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return targetStatement.getResultSet();
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return targetStatement.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return targetStatement.getMoreResults();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        targetStatement.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return targetStatement.getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        targetStatement.setFetchDirection(rows);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return targetStatement.getFetchSize();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return targetStatement.getResultSetConcurrency();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return targetStatement.getResultSetType();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        if (StringUtils.isNotBlank(targetSql)) {
            targetSql += "; " + sql;
        } else {
            targetSql = sql;
        }
        targetStatement.addBatch(sql);
    }

    @Override
    public void clearBatch() throws SQLException {
        targetStatement.clearBatch();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        return targetStatement.executeBatch();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return targetStatement.getConnection();
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return targetStatement.getMoreResults(current);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return targetStatement.getGeneratedKeys();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return targetStatement.executeUpdate(sql, autoGeneratedKeys);
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return targetStatement.executeUpdate(sql, columnIndexes);
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return targetStatement.executeUpdate(sql, columnNames);
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return targetStatement.execute(sql, autoGeneratedKeys);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return targetStatement.execute(sql, columnIndexes);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return targetStatement.execute(sql, columnNames);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return targetStatement.getResultSetHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return targetStatement.isClosed();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        targetStatement.setPoolable(poolable);
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return targetStatement.isPoolable();
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        targetStatement.closeOnCompletion();
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return targetStatement.isCloseOnCompletion();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return targetStatement.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return targetStatement.isWrapperFor(iface);
    }

    public ArrayList<Object>[] getParameters() {
        return parameters;
    }

    public void setParameters(ArrayList<Object>[] parameters) {
        this.parameters = parameters;
    }

    public PreparedStatement getTargetStatement() {
        return targetStatement;
    }

    public void setTargetStatement(PreparedStatement targetStatement) {
        this.targetStatement = targetStatement;
    }

    public String getTargetSql() {
        return targetSql;
    }

    public void setTargetSql(String targetSql) {
        this.targetSql = targetSql;
    }
}
