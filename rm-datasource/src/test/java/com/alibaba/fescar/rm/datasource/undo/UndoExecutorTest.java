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
package com.alibaba.fescar.rm.datasource.undo;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fescar.rm.datasource.sql.SQLType;
import com.alibaba.fescar.rm.datasource.sql.struct.Field;
import com.alibaba.fescar.rm.datasource.sql.struct.KeyType;
import com.alibaba.fescar.rm.datasource.sql.struct.Row;
import com.alibaba.fescar.rm.datasource.sql.struct.TableMeta;
import com.alibaba.fescar.rm.datasource.sql.struct.TableRecords;

import org.junit.Test;

/**
 * The type Undo executor test.
 */
public class UndoExecutorTest {

    /**
     * Test field.
     */
    @Test
    public void testField() {
        Field f = new Field();
        f.setName("name");
        f.setValue("x");
        f.setType(Types.VARCHAR);
        f.setKeyType(KeyType.PrimaryKey);

        String s = JSON.toJSONString(f, SerializerFeature.WriteDateUseDateFormat);

        System.out.println(s);

        Field fd = JSON.parseObject(s, Field.class);
        System.out.println(fd.getKeyType());
    }

    /**
     * Test update.
     */
    @Test
    public void testUpdate() {
        SQLUndoLog SQLUndoLog = new SQLUndoLog();
        SQLUndoLog.setTableName("my_test_table");
        SQLUndoLog.setSqlType(SQLType.UPDATE);

        TableRecords beforeImage = new TableRecords(new MockTableMeta("product", "id"));

        Row beforeRow = new Row();

        Field pkField = new Field();
        pkField.setKeyType(KeyType.PrimaryKey);
        pkField.setName("id");
        pkField.setType(Types.INTEGER);
        pkField.setValue(213);
        beforeRow.add(pkField);

        Field name = new Field();
        name.setName("name");
        name.setType(Types.VARCHAR);
        name.setValue("FESCAR");
        beforeRow.add(name);

        Field since = new Field();
        since.setName("since");
        since.setType(Types.VARCHAR);
        since.setValue("2014");
        beforeRow.add(since);

        beforeImage.add(beforeRow);

        TableRecords afterImage = new TableRecords(new MockTableMeta("product", "id"));

        Row afterRow = new Row();

        Field pkField1 = new Field();
        pkField1.setKeyType(KeyType.PrimaryKey);
        pkField1.setName("id");
        pkField1.setType(Types.INTEGER);
        pkField1.setValue(213);
        afterRow.add(pkField1);

        Field name1 = new Field();
        name1.setName("name");
        name1.setType(Types.VARCHAR);
        name1.setValue("GTS");
        afterRow.add(name1);

        Field since1 = new Field();
        since1.setName("since");
        since1.setType(Types.VARCHAR);
        since1.setValue("2016");
        afterRow.add(since1);

        afterImage.add(afterRow);

        SQLUndoLog.setBeforeImage(beforeImage);
        SQLUndoLog.setAfterImage(afterImage);

        AbstractUndoExecutor executor = UndoExecutorFactory.getUndoExecutor(JdbcConstants.MYSQL, SQLUndoLog);

        try {
            executor.executeOn(new MockConnection());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test insert.
     */
    @Test
    public void testInsert() {
        SQLUndoLog SQLUndoLog = new SQLUndoLog();
        SQLUndoLog.setTableName("my_test_table");
        SQLUndoLog.setSqlType(SQLType.INSERT);

        TableRecords beforeImage = TableRecords.empty(new MockTableMeta("product", "id"));

        TableRecords afterImage = new TableRecords(new MockTableMeta("product", "id"));

        Row afterRow1 = new Row();

        Field pkField = new Field();
        pkField.setKeyType(KeyType.PrimaryKey);
        pkField.setName("id");
        pkField.setType(Types.INTEGER);
        pkField.setValue(213);
        afterRow1.add(pkField);

        Field name = new Field();
        name.setName("name");
        name.setType(Types.VARCHAR);
        name.setValue("FESCAR");
        afterRow1.add(name);

        Field since = new Field();
        since.setName("since");
        since.setType(Types.VARCHAR);
        since.setValue("2014");
        afterRow1.add(since);

        Row afterRow = new Row();

        Field pkField1 = new Field();
        pkField1.setKeyType(KeyType.PrimaryKey);
        pkField1.setName("id");
        pkField1.setType(Types.INTEGER);
        pkField1.setValue(214);
        afterRow.add(pkField1);

        Field name1 = new Field();
        name1.setName("name");
        name1.setType(Types.VARCHAR);
        name1.setValue("GTS");
        afterRow.add(name1);

        Field since1 = new Field();
        since1.setName("since");
        since1.setType(Types.VARCHAR);
        since1.setValue("2016");
        afterRow.add(since1);

        afterImage.add(afterRow1);
        afterImage.add(afterRow);

        SQLUndoLog.setBeforeImage(beforeImage);
        SQLUndoLog.setAfterImage(afterImage);

        AbstractUndoExecutor executor = UndoExecutorFactory.getUndoExecutor(JdbcConstants.MYSQL, SQLUndoLog);

        try {
            executor.executeOn(new MockConnection());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test delete.
     */
    @Test
    public void testDelete() {
        SQLUndoLog SQLUndoLog = new SQLUndoLog();
        SQLUndoLog.setTableName("my_test_table");
        SQLUndoLog.setSqlType(SQLType.DELETE);

        TableRecords afterImage = TableRecords.empty(new MockTableMeta("product", "id"));

        TableRecords beforeImage = new TableRecords(new MockTableMeta("product", "id"));

        Row afterRow1 = new Row();

        Field pkField = new Field();
        pkField.setKeyType(KeyType.PrimaryKey);
        pkField.setName("id");
        pkField.setType(Types.INTEGER);
        pkField.setValue(213);
        afterRow1.add(pkField);

        Field name = new Field();
        name.setName("name");
        name.setType(Types.VARCHAR);
        name.setValue("FESCAR");
        afterRow1.add(name);

        Field since = new Field();
        since.setName("since");
        since.setType(Types.VARCHAR);
        since.setValue("2014");
        afterRow1.add(since);

        Row afterRow = new Row();

        Field pkField1 = new Field();
        pkField1.setKeyType(KeyType.PrimaryKey);
        pkField1.setName("id");
        pkField1.setType(Types.INTEGER);
        pkField1.setValue(214);
        afterRow.add(pkField1);

        Field name1 = new Field();
        name1.setName("name");
        name1.setType(Types.VARCHAR);
        name1.setValue("GTS");
        afterRow.add(name1);

        Field since1 = new Field();
        since1.setName("since");
        since1.setType(Types.VARCHAR);
        since1.setValue("2016");
        afterRow.add(since1);

        beforeImage.add(afterRow1);
        beforeImage.add(afterRow);

        SQLUndoLog.setAfterImage(afterImage);
        SQLUndoLog.setBeforeImage(beforeImage);

        AbstractUndoExecutor executor = UndoExecutorFactory.getUndoExecutor(JdbcConstants.MYSQL, SQLUndoLog);

        try {
            executor.executeOn(new MockConnection());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * The type Mock table meta.
     */
    public static class MockTableMeta extends TableMeta {

        private String mockPK;

        /**
         * Instantiates a new Mock table meta.
         *
         * @param tableName the table name
         * @param pkName    the pk name
         */
        public MockTableMeta(String tableName, String pkName) {
            setTableName(tableName);
            this.mockPK = pkName;

        }

        @Override
        public String getTableName() {
            return super.getTableName();
        }

        @Override
        public String getPkName() {
            return mockPK;
        }
    }

    /**
     * The type Mock connection.
     */
    public static class MockConnection implements Connection {

        @Override
        public Statement createStatement() throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql) throws SQLException {
            return new PreparedStatement() {
                @Override
                public ResultSet executeQuery() throws SQLException {
                    return null;
                }

                @Override
                public int executeUpdate() throws SQLException {
                    return 0;
                }

                @Override
                public void setNull(int parameterIndex, int sqlType) throws SQLException {

                }

                @Override
                public void setBoolean(int parameterIndex, boolean x) throws SQLException {

                }

                @Override
                public void setByte(int parameterIndex, byte x) throws SQLException {

                }

                @Override
                public void setShort(int parameterIndex, short x) throws SQLException {

                }

                @Override
                public void setInt(int parameterIndex, int x) throws SQLException {

                }

                @Override
                public void setLong(int parameterIndex, long x) throws SQLException {

                }

                @Override
                public void setFloat(int parameterIndex, float x) throws SQLException {

                }

                @Override
                public void setDouble(int parameterIndex, double x) throws SQLException {

                }

                @Override
                public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {

                }

                @Override
                public void setString(int parameterIndex, String x) throws SQLException {

                }

                @Override
                public void setBytes(int parameterIndex, byte[] x) throws SQLException {

                }

                @Override
                public void setDate(int parameterIndex, Date x) throws SQLException {

                }

                @Override
                public void setTime(int parameterIndex, Time x) throws SQLException {

                }

                @Override
                public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {

                }

                @Override
                public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {

                }

                @Override
                public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {

                }

                @Override
                public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {

                }

                @Override
                public void clearParameters() throws SQLException {

                }

                @Override
                public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {

                }

                @Override
                public void setObject(int parameterIndex, Object x) throws SQLException {

                }

                @Override
                public boolean execute() throws SQLException {
                    return false;
                }

                @Override
                public void addBatch() throws SQLException {

                }

                @Override
                public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {

                }

                @Override
                public void setRef(int parameterIndex, Ref x) throws SQLException {

                }

                @Override
                public void setBlob(int parameterIndex, Blob x) throws SQLException {

                }

                @Override
                public void setClob(int parameterIndex, Clob x) throws SQLException {

                }

                @Override
                public void setArray(int parameterIndex, Array x) throws SQLException {

                }

                @Override
                public ResultSetMetaData getMetaData() throws SQLException {
                    return null;
                }

                @Override
                public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {

                }

                @Override
                public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {

                }

                @Override
                public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {

                }

                @Override
                public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {

                }

                @Override
                public void setURL(int parameterIndex, URL x) throws SQLException {

                }

                @Override
                public ParameterMetaData getParameterMetaData() throws SQLException {
                    return null;
                }

                @Override
                public void setRowId(int parameterIndex, RowId x) throws SQLException {

                }

                @Override
                public void setNString(int parameterIndex, String value) throws SQLException {

                }

                @Override
                public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {

                }

                @Override
                public void setNClob(int parameterIndex, NClob value) throws SQLException {

                }

                @Override
                public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {

                }

                @Override
                public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {

                }

                @Override
                public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {

                }

                @Override
                public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {

                }

                @Override
                public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength)
                    throws SQLException {

                }

                @Override
                public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {

                }

                @Override
                public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {

                }

                @Override
                public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {

                }

                @Override
                public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {

                }

                @Override
                public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {

                }

                @Override
                public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {

                }

                @Override
                public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {

                }

                @Override
                public void setClob(int parameterIndex, Reader reader) throws SQLException {

                }

                @Override
                public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {

                }

                @Override
                public void setNClob(int parameterIndex, Reader reader) throws SQLException {

                }

                @Override
                public ResultSet executeQuery(String sql) throws SQLException {
                    return null;
                }

                @Override
                public int executeUpdate(String sql) throws SQLException {
                    return 0;
                }

                @Override
                public void close() throws SQLException {

                }

                @Override
                public int getMaxFieldSize() throws SQLException {
                    return 0;
                }

                @Override
                public void setMaxFieldSize(int max) throws SQLException {

                }

                @Override
                public int getMaxRows() throws SQLException {
                    return 0;
                }

                @Override
                public void setMaxRows(int max) throws SQLException {

                }

                @Override
                public void setEscapeProcessing(boolean enable) throws SQLException {

                }

                @Override
                public int getQueryTimeout() throws SQLException {
                    return 0;
                }

                @Override
                public void setQueryTimeout(int seconds) throws SQLException {

                }

                @Override
                public void cancel() throws SQLException {

                }

                @Override
                public SQLWarning getWarnings() throws SQLException {
                    return null;
                }

                @Override
                public void clearWarnings() throws SQLException {

                }

                @Override
                public void setCursorName(String name) throws SQLException {

                }

                @Override
                public boolean execute(String sql) throws SQLException {
                    return false;
                }

                @Override
                public ResultSet getResultSet() throws SQLException {
                    return null;
                }

                @Override
                public int getUpdateCount() throws SQLException {
                    return 0;
                }

                @Override
                public boolean getMoreResults() throws SQLException {
                    return false;
                }

                @Override
                public void setFetchDirection(int direction) throws SQLException {

                }

                @Override
                public int getFetchDirection() throws SQLException {
                    return 0;
                }

                @Override
                public void setFetchSize(int rows) throws SQLException {

                }

                @Override
                public int getFetchSize() throws SQLException {
                    return 0;
                }

                @Override
                public int getResultSetConcurrency() throws SQLException {
                    return 0;
                }

                @Override
                public int getResultSetType() throws SQLException {
                    return 0;
                }

                @Override
                public void addBatch(String sql) throws SQLException {

                }

                @Override
                public void clearBatch() throws SQLException {

                }

                @Override
                public int[] executeBatch() throws SQLException {
                    return new int[0];
                }

                @Override
                public Connection getConnection() throws SQLException {
                    return null;
                }

                @Override
                public boolean getMoreResults(int current) throws SQLException {
                    return false;
                }

                @Override
                public ResultSet getGeneratedKeys() throws SQLException {
                    return null;
                }

                @Override
                public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
                    return 0;
                }

                @Override
                public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
                    return 0;
                }

                @Override
                public int executeUpdate(String sql, String[] columnNames) throws SQLException {
                    return 0;
                }

                @Override
                public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
                    return false;
                }

                @Override
                public boolean execute(String sql, int[] columnIndexes) throws SQLException {
                    return false;
                }

                @Override
                public boolean execute(String sql, String[] columnNames) throws SQLException {
                    return false;
                }

                @Override
                public int getResultSetHoldability() throws SQLException {
                    return 0;
                }

                @Override
                public boolean isClosed() throws SQLException {
                    return false;
                }

                @Override
                public void setPoolable(boolean poolable) throws SQLException {

                }

                @Override
                public boolean isPoolable() throws SQLException {
                    return false;
                }

                @Override
                public void closeOnCompletion() throws SQLException {

                }

                @Override
                public boolean isCloseOnCompletion() throws SQLException {
                    return false;
                }

                @Override
                public <T> T unwrap(Class<T> iface) throws SQLException {
                    return null;
                }

                @Override
                public boolean isWrapperFor(Class<?> iface) throws SQLException {
                    return false;
                }
            };
        }

        @Override
        public CallableStatement prepareCall(String sql) throws SQLException {
            return null;
        }

        @Override
        public String nativeSQL(String sql) throws SQLException {
            return null;
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {

        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return false;
        }

        @Override
        public void commit() throws SQLException {

        }

        @Override
        public void rollback() throws SQLException {

        }

        @Override
        public void close() throws SQLException {

        }

        @Override
        public boolean isClosed() throws SQLException {
            return false;
        }

        @Override
        public DatabaseMetaData getMetaData() throws SQLException {
            return null;
        }

        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {

        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return false;
        }

        @Override
        public void setCatalog(String catalog) throws SQLException {

        }

        @Override
        public String getCatalog() throws SQLException {
            return null;
        }

        @Override
        public void setTransactionIsolation(int level) throws SQLException {

        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            return 0;
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            return null;
        }

        @Override
        public void clearWarnings() throws SQLException {

        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
            return null;
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
            return null;
        }

        @Override
        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return null;
        }

        @Override
        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {

        }

        @Override
        public void setHoldability(int holdability) throws SQLException {

        }

        @Override
        public int getHoldability() throws SQLException {
            return 0;
        }

        @Override
        public Savepoint setSavepoint() throws SQLException {
            return null;
        }

        @Override
        public Savepoint setSavepoint(String name) throws SQLException {
            return null;
        }

        @Override
        public void rollback(Savepoint savepoint) throws SQLException {

        }

        @Override
        public void releaseSavepoint(Savepoint savepoint) throws SQLException {

        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
                                                  int resultSetHoldability) throws SQLException {
            return null;
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
                                             int resultSetHoldability) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return null;
        }

        @Override
        public Clob createClob() throws SQLException {
            return null;
        }

        @Override
        public Blob createBlob() throws SQLException {
            return null;
        }

        @Override
        public NClob createNClob() throws SQLException {
            return null;
        }

        @Override
        public SQLXML createSQLXML() throws SQLException {
            return null;
        }

        @Override
        public boolean isValid(int timeout) throws SQLException {
            return false;
        }

        @Override
        public void setClientInfo(String name, String value) throws SQLClientInfoException {

        }

        @Override
        public void setClientInfo(Properties properties) throws SQLClientInfoException {

        }

        @Override
        public String getClientInfo(String name) throws SQLException {
            return null;
        }

        @Override
        public Properties getClientInfo() throws SQLException {
            return null;
        }

        @Override
        public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return null;
        }

        @Override
        public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return null;
        }

        @Override
        public void setSchema(String schema) throws SQLException {

        }

        @Override
        public String getSchema() throws SQLException {
            return null;
        }

        @Override
        public void abort(Executor executor) throws SQLException {

        }

        @Override
        public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {

        }

        @Override
        public int getNetworkTimeout() throws SQLException {
            return 0;
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return false;
        }
    }

}
