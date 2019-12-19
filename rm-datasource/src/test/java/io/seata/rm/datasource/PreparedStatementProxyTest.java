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
package io.seata.rm.datasource;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import com.alibaba.druid.mock.MockArray;
import com.alibaba.druid.mock.MockNClob;
import com.alibaba.druid.mock.MockRef;
import com.alibaba.druid.mock.MockSQLXML;
import com.alibaba.druid.pool.DruidDataSource;

import com.google.common.collect.Lists;
import io.seata.rm.datasource.mock.MockBlob;
import io.seata.rm.datasource.mock.MockClob;
import io.seata.rm.datasource.mock.MockConnection;
import io.seata.rm.datasource.mock.MockDriver;
import io.seata.rm.datasource.sql.struct.Null;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author will
 */
public class PreparedStatementProxyTest {

    private static List<String> returnValueColumnLabels = Lists.newArrayList("id", "name");

    private static Object[][] returnValue = new Object[][] {
        new Object[] {1, "Tom"},
        new Object[] {2, "Jack"},
    };

    private static Object[][] columnMetas = new Object[][] {
        new Object[] {"", "", "table_prepared_statement_proxy", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
        new Object[] {"", "", "table_prepared_statement_proxy", "name", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
    };

    private static Object[][] indexMetas = new Object[][] {
        new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},
    };

    private static PreparedStatementProxy preparedStatementProxy;

    private static TestUnusedConstructorPreparedStatementProxy unusedConstructorPreparedStatementProxy;

    @BeforeAll
    public static void init() throws SQLException {
        MockDriver mockDriver = new MockDriver(returnValueColumnLabels, returnValue, columnMetas, indexMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy dataSourceProxy = new DataSourceProxy(dataSource);

        ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, dataSource.getConnection().getConnection());

        String sql = "update from prepared_statement_proxy set name = ?";

        PreparedStatement preparedStatement = mockDriver.createSeataMockPreparedStatement(
            (MockConnection)connectionProxy.getTargetConnection(), sql);

        preparedStatementProxy = new PreparedStatementProxy(connectionProxy, preparedStatement, sql);
        unusedConstructorPreparedStatementProxy = new TestUnusedConstructorPreparedStatementProxy(connectionProxy, preparedStatement);
    }

    @Test
    public void testPreparedStatementProxy() {
        Assertions.assertNotNull(preparedStatementProxy);
        Assertions.assertNotNull(unusedConstructorPreparedStatementProxy);
    }

    @Test
    public void testExecute() throws SQLException {
        Assertions.assertNotNull(preparedStatementProxy.execute());
    }

    @Test
    public void testExecuteUpdate() throws SQLException {
        Assertions.assertNotNull(preparedStatementProxy.executeUpdate());
    }

    @Test
    public void testExecuteQuery() throws SQLException {
        Assertions.assertNotNull(preparedStatementProxy.executeQuery());
    }

    @Test
    public void testGetSetParamsByIndex() {
        preparedStatementProxy.setParamByIndex(1, "xxx");
        Assertions.assertEquals("xxx",  preparedStatementProxy.getParamsByIndex(0).get(0));
    }

    @Test
    public void testSetParam() throws SQLException, MalformedURLException {
        preparedStatementProxy.clearParameters();
        preparedStatementProxy.setNull(1, JDBCType.DECIMAL.getVendorTypeNumber());
        Assertions.assertEquals(Null.get(), preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setNull(1, JDBCType.DECIMAL.getVendorTypeNumber(), "NULL");
        Assertions.assertEquals(Null.get(), preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setBoolean(1, true);
        Assertions.assertEquals(true, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setByte(1, (byte)0);
        Assertions.assertEquals((byte)0, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setShort(1, (short)0);
        Assertions.assertEquals((short)0, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setInt(1, 0);
        Assertions.assertEquals(0, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setLong(1, 0L);
        Assertions.assertEquals(0L, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setFloat(1, 0f);
        Assertions.assertEquals(0f, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setDouble(1, 1.1);
        Assertions.assertEquals(1.1, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setBigDecimal(1, new BigDecimal(0));
        Assertions.assertEquals(new BigDecimal(0), preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setString(1, "x");
        Assertions.assertEquals("x", preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setNString(1, "x");
        Assertions.assertEquals("x", preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setBytes(1, "x".getBytes());
        Assertions.assertTrue(Objects.deepEquals("x".getBytes(), preparedStatementProxy.getParamsByIndex(0).get(0)));
        preparedStatementProxy.clearParameters();

        Date date = new Date(System.currentTimeMillis());
        preparedStatementProxy.setDate(1, date);
        Assertions.assertEquals(date, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setDate(1, date, Calendar.getInstance());
        Assertions.assertEquals(date, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        Time time = new Time(System.currentTimeMillis());
        preparedStatementProxy.setTime(1, time);
        Assertions.assertEquals(time, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setTime(1, time, Calendar.getInstance());
        Assertions.assertEquals(time, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        preparedStatementProxy.setTimestamp(1, timestamp);
        Assertions.assertEquals(timestamp, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setTimestamp(1, timestamp, Calendar.getInstance());
        Assertions.assertEquals(timestamp, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream("x".getBytes(), 0, 1);
        preparedStatementProxy.setAsciiStream(1, byteArrayInputStream);
        Assertions.assertEquals(byteArrayInputStream, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setAsciiStream(1, byteArrayInputStream, 1L);
        Assertions.assertEquals(byteArrayInputStream, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setAsciiStream(1, byteArrayInputStream);
        Assertions.assertEquals(byteArrayInputStream, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setUnicodeStream(1, byteArrayInputStream, 1);
        Assertions.assertEquals(byteArrayInputStream, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setBinaryStream(1, byteArrayInputStream);
        Assertions.assertEquals(byteArrayInputStream, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setBinaryStream(1, byteArrayInputStream, 1L);
        Assertions.assertEquals(byteArrayInputStream, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setBinaryStream(1, byteArrayInputStream, 1);
        Assertions.assertEquals(byteArrayInputStream, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setObject(1, 1, JDBCType.INTEGER.getVendorTypeNumber());
        Assertions.assertEquals(1, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setObject(1, 1, JDBCType.INTEGER.getVendorTypeNumber(), 1);
        Assertions.assertEquals(1, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setObject(1, 1);
        Assertions.assertEquals(1, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        Assertions.assertDoesNotThrow(() -> {
            preparedStatementProxy.addBatch();
        });

        CharArrayReader charArrayReader = new CharArrayReader("x".toCharArray());
        preparedStatementProxy.setCharacterStream(1, charArrayReader, 1);
        Assertions.assertEquals(charArrayReader, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setCharacterStream(1, charArrayReader, 1L);
        Assertions.assertEquals(charArrayReader, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setCharacterStream(1, charArrayReader);
        Assertions.assertEquals(charArrayReader, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setNCharacterStream(1, charArrayReader);
        Assertions.assertEquals(charArrayReader, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setNCharacterStream(1, charArrayReader, 1L);
        Assertions.assertEquals(charArrayReader, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        MockRef ref = new MockRef();
        preparedStatementProxy.setRef(1, ref);
        Assertions.assertEquals(ref, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        MockBlob blob = new MockBlob();
        preparedStatementProxy.setBlob(1, blob);
        Assertions.assertEquals(blob, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setBlob(1, byteArrayInputStream);
        Assertions.assertEquals(byteArrayInputStream, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setBlob(1, byteArrayInputStream, 1L);
        Assertions.assertEquals(byteArrayInputStream, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        MockClob clob = new MockClob();
        preparedStatementProxy.setClob(1, clob);
        Assertions.assertEquals(clob, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setClob(1, charArrayReader, 1L);
        Assertions.assertEquals(charArrayReader, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setClob(1, charArrayReader);
        Assertions.assertEquals(charArrayReader, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        MockNClob nclob = new MockNClob();
        preparedStatementProxy.setNClob(1, nclob);
        Assertions.assertEquals(nclob, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setNClob(1, charArrayReader, 1L);
        Assertions.assertEquals(charArrayReader, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        preparedStatementProxy.setNClob(1, charArrayReader);
        Assertions.assertEquals(charArrayReader, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        MockArray array = new MockArray();
        preparedStatementProxy.setArray(1, array);
        Assertions.assertEquals(array, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        Assertions.assertNotNull(preparedStatementProxy.getMetaData());
        Assertions.assertNotNull(preparedStatementProxy.getParameterMetaData());

        URL url = new URL("http", "", 8080, "");
        preparedStatementProxy.setURL(1, url);
        Assertions.assertEquals(url, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        MockSQLXML sqlxml = new MockSQLXML();
        preparedStatementProxy.setSQLXML(1, sqlxml);
        Assertions.assertEquals(sqlxml, preparedStatementProxy.getParamsByIndex(0).get(0));
        preparedStatementProxy.clearParameters();

        Assertions.assertNotNull(preparedStatementProxy.getParameters());
    }

    /**
     * This class use for test the unused constructor in AbstractPreparedStatementProxy
     */
    private static class TestUnusedConstructorPreparedStatementProxy extends AbstractPreparedStatementProxy {

        public TestUnusedConstructorPreparedStatementProxy(AbstractConnectionProxy connectionProxy, PreparedStatement targetStatement) throws SQLException {
            super(connectionProxy, targetStatement);
        }

        @Override
        public ResultSet executeQuery() throws SQLException {
            return null;
        }

        @Override
        public int executeUpdate() throws SQLException {
            return 0;
        }

        @Override
        public boolean execute() throws SQLException {
            return false;
        }
    }
}
