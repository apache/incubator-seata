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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * PreparedStatement proxy for XA mode.
 *
 * @author sharajava
 */
public class PreparedStatementProxyXA extends StatementProxyXA implements PreparedStatement {

    public PreparedStatementProxyXA(AbstractConnectionProxyXA connectionProxyXA, PreparedStatement targetStatement) {
        super(connectionProxyXA, targetStatement);
    }

    private PreparedStatement getTargetStatement() {
        return (PreparedStatement)targetStatement;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        return ExecuteTemplateXA.execute(connectionProxyXA, (statement, args) -> statement.executeQuery(), getTargetStatement());
    }

    @Override
    public int executeUpdate() throws SQLException {
        return ExecuteTemplateXA.execute(connectionProxyXA, (statement, args) -> statement.executeUpdate(), getTargetStatement());
    }

    @Override
    public boolean execute() throws SQLException {
        return ExecuteTemplateXA.execute(connectionProxyXA, (statement, args) -> statement.execute(), getTargetStatement());
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        getTargetStatement().setNull(parameterIndex, sqlType);

    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        getTargetStatement().setBoolean(parameterIndex, x);

    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        getTargetStatement().setByte(parameterIndex, x);

    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        getTargetStatement().setShort(parameterIndex, x);

    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        getTargetStatement().setInt(parameterIndex, x);

    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        getTargetStatement().setLong(parameterIndex, x);

    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        getTargetStatement().setFloat(parameterIndex, x);

    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        getTargetStatement().setDouble(parameterIndex, x);

    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        getTargetStatement().setBigDecimal(parameterIndex, x);

    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        getTargetStatement().setString(parameterIndex, x);

    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        getTargetStatement().setBytes(parameterIndex, x);
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        getTargetStatement().setDate(parameterIndex, x);

    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        getTargetStatement().setTime(parameterIndex, x);

    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        getTargetStatement().setTimestamp(parameterIndex, x);

    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        getTargetStatement().setAsciiStream(parameterIndex, x);

    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        getTargetStatement().setUnicodeStream(parameterIndex, x, length);

    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        getTargetStatement().setBinaryStream(parameterIndex, x, length);

    }

    @Override
    public void clearParameters() throws SQLException {
        getTargetStatement().clearParameters();

    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        getTargetStatement().setObject(parameterIndex, x, targetSqlType);

    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        getTargetStatement().setObject(parameterIndex, x);

    }

    @Override
    public void addBatch() throws SQLException {
        getTargetStatement().addBatch();
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        getTargetStatement().setCharacterStream(parameterIndex, reader, length);
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        getTargetStatement().setRef(parameterIndex, x);

    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        getTargetStatement().setBlob(parameterIndex, x);

    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        getTargetStatement().setClob(parameterIndex, x);

    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        getTargetStatement().setArray(parameterIndex, x);

    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return getTargetStatement().getMetaData();
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        getTargetStatement().setDate(parameterIndex, x, cal);
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        getTargetStatement().setTime(parameterIndex, x, cal);

    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        getTargetStatement().setTimestamp(parameterIndex, x, cal);

    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        getTargetStatement().setNull(parameterIndex, sqlType, typeName);

    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        getTargetStatement().setURL(parameterIndex, x);

    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return getTargetStatement().getParameterMetaData();
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        getTargetStatement().setRowId(parameterIndex, x);

    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        getTargetStatement().setNString(parameterIndex, value);

    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        getTargetStatement().setNCharacterStream(parameterIndex, value, length);
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        getTargetStatement().setNClob(parameterIndex, value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        getTargetStatement().setClob(parameterIndex, reader, length);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        getTargetStatement().setBlob(parameterIndex, inputStream, length);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        getTargetStatement().setNClob(parameterIndex, reader, length);
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        getTargetStatement().setSQLXML(parameterIndex, xmlObject);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        getTargetStatement().setObject(parameterIndex, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        getTargetStatement().setAsciiStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        getTargetStatement().setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        getTargetStatement().setCharacterStream(parameterIndex, reader, length);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        getTargetStatement().setAsciiStream(parameterIndex, x);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        getTargetStatement().setBinaryStream(parameterIndex, x);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        getTargetStatement().setCharacterStream(parameterIndex, reader);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        getTargetStatement().setNCharacterStream(parameterIndex, value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        getTargetStatement().setClob(parameterIndex, reader);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        getTargetStatement().setBlob(parameterIndex, inputStream);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        getTargetStatement().setNClob(parameterIndex, reader);
    }
}
