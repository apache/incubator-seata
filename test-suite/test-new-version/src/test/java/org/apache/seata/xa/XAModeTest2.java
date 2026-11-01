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
package org.apache.seata.xa;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.xa.DruidXADataSource;
import com.alibaba.druid.util.JdbcUtils;
import com.mysql.cj.jdbc.MysqlXADataSource;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.Resource;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.rm.datasource.xa.AbstractDataSourceProxyXA;
import org.apache.seata.rm.datasource.xa.DataSourceProxyXA;
import org.apache.seata.rm.datasource.xa.DataSourceProxyXANative;
import org.apache.seata.rm.datasource.xa.ResourceManagerXA;
import org.apache.seata.rm.datasource.xa.XAXid;
import org.apache.seata.rm.datasource.xa.XAXidBuilder;
import org.apache.seata.spring.annotation.GlobalTransactionScanner;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.apache.seata.tm.api.GlobalTransaction;
import org.apache.seata.tm.api.GlobalTransactionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.postgresql.xa.PGXADataSource;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class XAModeTest2 {

    private static final int TEST_RECORD_ID = 888;
    private static final String TEST_RECORD_NAME = "xxx";
    private static final long TEST_TID = 1582688600006L;
    private static final String MOCK_XID = "127.0.0.1:8091:" + TEST_TID;
    private static final long MOCK_BRANCH_ID = TEST_TID + 1;

    private static final String PG_JDBC_URL = "jdbc:postgresql://127.0.0.1:5432/postgres";
    private static final String PG_USERNAME = "postgres";
    private static final String PG_PASSWORD = "postgres";
    private static final String PG_DRIVER_CLASS_NAME = JdbcUtils.POSTGRESQL_DRIVER;

    private static final String MYSQL_JDBC_URL = "jdbc:mysql://127.0.0.1:3306/demo";
    private static final String MYSQL_USERNAME = "demo";
    private static final String MYSQL_PASSWORD = "demo";
    private static final String MYSQL_DRIVER_CLASS_NAME = JdbcUtils.MYSQL_DRIVER;

    private static final String MYSQL8_JDBC_URL =
            "jdbc:mysql://0.0.0.0:3306/demo?useUnicode=true&characterEncoding=utf-8&useSSL=false";
    private static final String MYSQL8_USERNAME = "demo";
    private static final String MYSQL8_PASSWORD = "demo";
    private static final String MYSQL8_DRIVER_CLASS_NAME = JdbcUtils.MYSQL_DRIVER_6;

    private static final String ORACLE_JDBC_URL = "jdbc:oracle:thin:@localhost:1521:xe";
    private static final String ORACLE_USERNAME = "demo";
    private static final String ORACLE_PASSWORD = "demo";
    private static final String ORACLE_DRIVER_CLASS_NAME = JdbcUtils.ORACLE_DRIVER;

    // Test on different DB, including: MySQL(5.7, 8.0), PostgreSQL(11), Oracle(11)
    private static final String DB_TYPE = JdbcConstants.MYSQL;

    private static final boolean NATIVE_XA = false;

    private static final boolean MYSQL8 = false;

    private DruidDataSource createNewDruidDataSource() throws Throwable {
        DruidDataSource druidDataSource = new DruidDataSource();
        initDruidDataSource(druidDataSource);
        return druidDataSource;
    }

    private DruidXADataSource createNewDruidXADataSource() throws Throwable {
        DruidXADataSource druidDataSource = new DruidXADataSource();
        initDruidDataSource(druidDataSource);
        return druidDataSource;
    }

    private XADataSource createNewNativeXADataSource() throws Throwable {
        if (DB_TYPE.equalsIgnoreCase(JdbcConstants.POSTGRESQL)) {
            PGXADataSource pgxaDataSource = new PGXADataSource();
            pgxaDataSource.setUrl(PG_JDBC_URL);
            pgxaDataSource.setUser(PG_USERNAME);
            pgxaDataSource.setPassword(PG_PASSWORD);
            return pgxaDataSource;

        } else if (DB_TYPE.equalsIgnoreCase(JdbcConstants.MYSQL)) {
            MysqlXADataSource mysqlXADataSource = new MysqlXADataSource();
            if (MYSQL8) {
                mysqlXADataSource.setURL(MYSQL8_JDBC_URL);
                mysqlXADataSource.setUser(MYSQL8_USERNAME);
                mysqlXADataSource.setPassword(MYSQL8_USERNAME);

            } else {
                mysqlXADataSource.setURL(MYSQL_JDBC_URL);
                mysqlXADataSource.setUser(MYSQL_USERNAME);
                mysqlXADataSource.setPassword(MYSQL_USERNAME);
            }
            return mysqlXADataSource;

        } else if (DB_TYPE.equalsIgnoreCase(JdbcConstants.ORACLE)) {
            return createOracleXADataSource();

        } else {
            throw new IllegalAccessError("Unknown DB_TYPE: " + DB_TYPE);
        }
    }

    private XADataSource createOracleXADataSource() {
        try {
            Class oracleXADataSourceClass = Class.forName("oracle.jdbc.xa.client.OracleXADataSource");
            XADataSource xaDataSource = (XADataSource) oracleXADataSourceClass.newInstance();

            Method setURLMethod = oracleXADataSourceClass.getMethod("setURL", String.class);
            setURLMethod.invoke(xaDataSource, ORACLE_JDBC_URL);

            Method setUserMethod = oracleXADataSourceClass.getMethod("setUser", String.class);
            setUserMethod.invoke(xaDataSource, ORACLE_USERNAME);

            Method setPasswordMethod = oracleXADataSourceClass.getMethod("setPassword", String.class);
            setPasswordMethod.invoke(xaDataSource, ORACLE_PASSWORD);

            Method setDriverTypeMethod = oracleXADataSourceClass.getMethod("setDriverType", String.class);
            setDriverTypeMethod.invoke(xaDataSource, ORACLE_DRIVER_CLASS_NAME);

            return xaDataSource;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void initDruidDataSource(DruidDataSource druidDataSource) throws Throwable {
        druidDataSource.setDbType(DB_TYPE);
        if (DB_TYPE.equalsIgnoreCase(JdbcConstants.POSTGRESQL)) {
            druidDataSource.setUrl(PG_JDBC_URL);
            druidDataSource.setUsername(PG_USERNAME);
            druidDataSource.setPassword(PG_PASSWORD);
            druidDataSource.setDriverClassName(PG_DRIVER_CLASS_NAME);

        } else if (DB_TYPE.equalsIgnoreCase(JdbcConstants.MYSQL)) {
            if (MYSQL8) {
                druidDataSource.setUrl(MYSQL8_JDBC_URL);
                druidDataSource.setUsername(MYSQL8_USERNAME);
                druidDataSource.setPassword(MYSQL8_PASSWORD);
                druidDataSource.setDriverClassName(MYSQL8_DRIVER_CLASS_NAME);

            } else {
                druidDataSource.setUrl(MYSQL_JDBC_URL);
                druidDataSource.setUsername(MYSQL_USERNAME);
                druidDataSource.setPassword(MYSQL_PASSWORD);
                druidDataSource.setDriverClassName(MYSQL_DRIVER_CLASS_NAME);
            }

        } else if (DB_TYPE.equalsIgnoreCase(JdbcConstants.ORACLE)) {
            druidDataSource.setUrl(ORACLE_JDBC_URL);
            druidDataSource.setUsername(ORACLE_USERNAME);
            druidDataSource.setPassword(ORACLE_PASSWORD);
            druidDataSource.setDriverClassName(ORACLE_DRIVER_CLASS_NAME);

        } else {
            throw new IllegalAccessError("Unknown DB_TYPE: " + DB_TYPE);
        }
        druidDataSource.init();
    }

    private void initRM() throws Throwable {
        // init RM
        DefaultResourceManager.get();
        // mock the RM of XA
        DefaultResourceManager.mockResourceManager(BranchType.XA, new ResourceManagerXA() {
            @Override
            public void registerResource(Resource resource) {
                dataSourceCache.put(resource.getResourceId(), resource);
            }

            @Override
            public Long branchRegister(
                    BranchType branchType,
                    String resourceId,
                    String clientId,
                    String xid,
                    String applicationData,
                    String lockKeys)
                    throws TransactionException {
                return MOCK_BRANCH_ID;
            }

            @Override
            public void branchReport(
                    BranchType branchType, String xid, long branchId, BranchStatus status, String applicationData)
                    throws TransactionException {}
        });
    }

    @Test
    @Disabled
    public void testAllInOne() throws Throwable {
        testCleanXARecover();
        doTestXAModeNormalPrepareData();
        doTestXAModeNormalCaseAllInOne(MOCK_XID, MOCK_BRANCH_ID);
    }

    @Test
    @Disabled
    public void testGlobalCommitOnDifferentDataSource() throws Throwable {
        testCleanXARecover();
        doTestXAModeNormalPrepareData();
        doTestXAModeNormalCasePhase1(MOCK_XID, MOCK_BRANCH_ID);
        // Use new DataSource in phase 2
        doTestXAModeNormalCasePhase2(true, MOCK_XID, MOCK_BRANCH_ID);
    }

    @Test
    @Disabled
    public void testGlobalRollbackOnDifferentDataSource() throws Throwable {
        testCleanXARecover();
        doTestXAModeNormalPrepareData();
        doTestXAModeNormalCasePhase1(MOCK_XID, MOCK_BRANCH_ID);
        // Use new DataSource in phase 2
        doTestXAModeNormalCasePhase2(false, MOCK_XID, MOCK_BRANCH_ID);
    }

    @Test
    @Disabled
    public void testOnlyPhase1() throws Throwable {
        testCleanXARecover();
        doTestXAModeNormalPrepareData();
        doTestXAModeNormalCasePhase1(MOCK_XID, MOCK_BRANCH_ID);
    }

    @Test
    @Disabled
    public void testOnlyPhase2Commit() throws Throwable {
        doTestXAModeNormalCasePhase2(true, MOCK_XID, MOCK_BRANCH_ID);
    }

    @Test
    @Disabled
    public void testOnlyPhase2Rollback() throws Throwable {
        doTestXAModeNormalCasePhase2(false, MOCK_XID, MOCK_BRANCH_ID);
    }

    private void doTestXAModeNormalPrepareData() throws Throwable {
        // init DataSource: helper
        DruidDataSource helperDS = createNewDruidDataSource();

        // prepare data for test: make sure no test record there
        Connection helperConn = helperDS.getConnection();
        Statement helperStat = helperConn.createStatement();
        ResultSet helperRes = null;
        helperStat.execute("delete from test where id = " + TEST_RECORD_ID);
        helperStat.close();
        helperConn.close();
    }

    private void doTestXAModeNormalCasePhase2(boolean globalCommit, String mockXid, Long mockBranchId)
            throws Throwable {
        // init DataSource: helper
        DruidDataSource helperDS = createNewDruidDataSource();

        Connection helperConn = null;
        Statement helperStat = null;
        ResultSet helperRes = null;

        // init RM
        initRM();

        AbstractDataSourceProxyXA dataSourceProxyXA = null;
        if (NATIVE_XA) {
            // init XADataSource runnerXA
            XADataSource runnerXADS = createNewNativeXADataSource();
            dataSourceProxyXA = new DataSourceProxyXANative(runnerXADS);
        } else {
            // init DataSource: runner
            DruidDataSource runnerDS = createNewDruidDataSource();
            dataSourceProxyXA = new DataSourceProxyXA(runnerDS);
        }

        // Global Tx Phase 2:
        if (globalCommit) {
            DefaultResourceManager.get()
                    .branchCommit(
                            dataSourceProxyXA.getBranchType(),
                            mockXid,
                            mockBranchId,
                            dataSourceProxyXA.getResourceId(),
                            null);

            // have a check
            helperConn = helperDS.getConnection();
            helperStat = helperConn.createStatement();
            helperRes = helperStat.executeQuery("select * from test where id = " + TEST_RECORD_ID);
            // should see the test record now
            Assertions.assertTrue(helperRes.next());
            Assertions.assertEquals(helperRes.getInt(1), TEST_RECORD_ID);
            Assertions.assertEquals(helperRes.getString(2), TEST_RECORD_NAME);
            helperRes.close();
            helperStat.close();
            helperConn.close();

        } else {
            DefaultResourceManager.get()
                    .branchRollback(
                            dataSourceProxyXA.getBranchType(),
                            mockXid,
                            mockBranchId,
                            dataSourceProxyXA.getResourceId(),
                            null);

            // have a check
            helperConn = helperDS.getConnection();
            helperStat = helperConn.createStatement();
            helperRes = helperStat.executeQuery("select * from test where id = " + TEST_RECORD_ID);
            // should NOT see the test record now
            Assertions.assertFalse(helperRes.next());
            helperRes.close();
            helperStat.close();
            helperConn.close();
        }
        System.out.println("Phase2 looks good!");
    }

    private void doTestXAModeNormalCasePhase1(String mockXid, Long mockBranchId) throws Throwable {
        // init DataSource: helper
        DruidDataSource helperDS = createNewDruidDataSource();

        Connection helperConn = null;
        Statement helperStat = null;
        ResultSet helperRes = null;

        // init RM
        initRM();

        AbstractDataSourceProxyXA dataSourceProxyXA = null;
        if (NATIVE_XA) {
            // init XADataSource runnerXA
            XADataSource runnerXADS = createNewNativeXADataSource();
            dataSourceProxyXA = new DataSourceProxyXANative(runnerXADS);
        } else {
            // init DataSource: runner
            DruidDataSource runnerDS = createNewDruidDataSource();
            dataSourceProxyXA = new DataSourceProxyXA(runnerDS);
        }

        // Global Tx Phase 1:
        RootContext.bind(mockXid);
        Connection testConn = dataSourceProxyXA.getConnection();
        Statement testStat = testConn.createStatement();
        // >>> insert the test record with XA mode
        testStat.execute("insert into test(id, name) values(" + TEST_RECORD_ID + ", '" + TEST_RECORD_NAME + "')");
        // >>> close the statement and connection
        testStat.close();
        testConn.close();
        RootContext.unbind();

        // have a check
        helperConn = helperDS.getConnection();
        helperStat = helperConn.createStatement();
        helperRes = helperStat.executeQuery("select * from test where id = " + TEST_RECORD_ID);
        // should NOT see the record(id=888) now
        Assertions.assertFalse(helperRes.next());
        helperRes.close();
        helperStat.close();
        helperConn.close();

        if (JdbcConstants.MYSQL.equalsIgnoreCase(DB_TYPE)) {
            XAXid xaXid = XAXidBuilder.build(mockXid, mockBranchId);
            dataSourceProxyXA.forceClosePhysicalConnection(xaXid);
        }

        System.out.println("Phase1 looks good!");
    }

    private void doTestXAModeNormalCaseAllInOne(String mockXid, Long mockBranchId) throws Throwable {
        // init DataSource: helper
        DruidDataSource helperDS = createNewDruidDataSource();

        Connection helperConn = null;
        Statement helperStat = null;
        ResultSet helperRes = null;

        // init RM
        initRM();

        AbstractDataSourceProxyXA dataSourceProxyXA = null;
        if (NATIVE_XA) {
            // init XADataSource runnerXA
            XADataSource runnerXADS = createNewNativeXADataSource();
            dataSourceProxyXA = new DataSourceProxyXANative(runnerXADS);
        } else {
            // init DataSource: runner
            DruidDataSource runnerDS = createNewDruidDataSource();
            dataSourceProxyXA = new DataSourceProxyXA(runnerDS);
        }

        // Global Tx Phase 1:
        RootContext.bind(mockXid);
        Connection testConn = dataSourceProxyXA.getConnection();
        Statement testStat = testConn.createStatement();
        // >>> insert the test record with XA mode
        testStat.execute("insert into test(id, name) values(" + TEST_RECORD_ID + ", '" + TEST_RECORD_NAME + "')");
        // >>> close the statement and connection
        testStat.close();
        testConn.close();
        RootContext.unbind();

        // have a check
        helperConn = helperDS.getConnection();
        helperStat = helperConn.createStatement();
        helperRes = helperStat.executeQuery("select * from test where id = " + TEST_RECORD_ID);
        // should NOT see the record(id=888) now
        Assertions.assertFalse(helperRes.next());
        helperRes.close();
        helperStat.close();
        helperConn.close();

        // Global Tx Phase 2: run phase 2 with the same runner DS
        DefaultResourceManager.get()
                .branchCommit(
                        dataSourceProxyXA.getBranchType(),
                        mockXid,
                        mockBranchId,
                        dataSourceProxyXA.getResourceId(),
                        null);

        // have a check
        helperConn = helperDS.getConnection();
        helperStat = helperConn.createStatement();
        helperRes = helperStat.executeQuery("select * from test where id = " + TEST_RECORD_ID);
        // should see the test record now
        Assertions.assertTrue(helperRes.next());
        Assertions.assertEquals(helperRes.getInt(1), TEST_RECORD_ID);
        Assertions.assertEquals(helperRes.getString(2), TEST_RECORD_NAME);
        helperRes.close();
        helperStat.close();
        helperConn.close();

        System.out.println("All in one looks good!");
    }

    @Test
    @Disabled
    public void testXid() throws Throwable {
        XAXid xaXid = XAXidBuilder.build(MOCK_XID, MOCK_BRANCH_ID);

        XAXid retrievedXAXid = XAXidBuilder.build(xaXid.getGlobalTransactionId(), xaXid.getBranchQualifier());
        String retrievedXid = retrievedXAXid.getGlobalXid();
        long retrievedBranchId = retrievedXAXid.getBranchId();

        Assertions.assertEquals(MOCK_XID, retrievedXid);
        Assertions.assertEquals(MOCK_BRANCH_ID, retrievedBranchId);
    }

    @Test
    @Disabled
    public void testCleanXARecover() throws Throwable {
        XADataSource xaDataSource = createNewNativeXADataSource();

        XAConnection xaConnection = xaDataSource.getXAConnection();
        XAResource xaResource = xaConnection.getXAResource();

        Xid[] xids = xaResource.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
        for (Xid xid : xids) {
            try {
                xaResource.rollback(xid);
            } catch (XAException xae) {
                xae.printStackTrace();
            }
        }
        System.out.println("Unfinished XA branches are ALL cleaned!");
    }

    @Test
    @Disabled
    public void testXADataSourceNative() throws Throwable {
        XADataSource nativeXADataSource = createOracleXADataSource();

        XAConnection xaConnection = nativeXADataSource.getXAConnection();
        XAResource xaResource = xaConnection.getXAResource();
        Xid xid = XAXidBuilder.build("127.0.0.1:8091:1234", 1235L);
        xaResource.start(xid, XAResource.TMNOFLAGS);
    }

    @Test
    @Disabled
    public void testXADataSourceNormal() throws Throwable {
        DruidXADataSource druidDataSource = new DruidXADataSource();
        druidDataSource.setUrl(ORACLE_JDBC_URL);
        druidDataSource.setUsername(ORACLE_USERNAME);
        druidDataSource.setPassword(ORACLE_PASSWORD);
        druidDataSource.setDriverClassName(ORACLE_DRIVER_CLASS_NAME);

        XAConnection xaConnection = druidDataSource.getXAConnection();
        XAResource xaResource = xaConnection.getXAResource();
        Xid xid = XAXidBuilder.build("127.0.0.1:8091:1234", 1235L);
        // Since issue of Druid(https://github.com/alibaba/druid/issues/3707), XA start will fail.
        xaResource.start(xid, XAResource.TMNOFLAGS);
    }

    @Test
    @Disabled
    // Should RUN with local Seata Server
    public void testStandardAppGlobalCommit() throws Throwable {
        testCleanXARecover();
        doTestXAModeNormalPrepareData();

        // Create a standard proxy according to non-XA data source
        DataSource ds = createDataSourceProxyXA();
        // Create a global tx
        GlobalTransaction gtx = createGlobalTransaction();

        gtx.begin();
        runInGlobalTx(ds);
        gtx.commit();

        Thread.sleep(5000);
    }

    @Test
    @Disabled
    // Should RUN with local Seata Server
    public void testXANativeAppGlobalCommit() throws Throwable {
        testCleanXARecover();
        doTestXAModeNormalPrepareData();

        // Create a native proxy according to XA data source
        DataSource ds = createDataSourceProxyXANative();
        // Create a global tx
        GlobalTransaction gtx = createGlobalTransaction();

        gtx.begin();
        runInGlobalTx(ds);
        gtx.commit();

        Thread.sleep(5000);
    }

    @Test
    @Disabled
    // Should RUN with local Seata Server
    public void testStandardAppGlobalRollback() throws Throwable {
        testCleanXARecover();
        doTestXAModeNormalPrepareData();

        // Create a standard proxy according to non-XA data source
        DataSource ds = createDataSourceProxyXA();
        // Create a global tx
        GlobalTransaction gtx = createGlobalTransaction();

        gtx.begin();
        runInGlobalTx(ds);
        gtx.rollback();

        Thread.sleep(5000);
    }

    @Test
    @Disabled
    // Should RUN with local Seata Server
    public void testXANativeAppGlobalRollback() throws Throwable {
        testCleanXARecover();
        doTestXAModeNormalPrepareData();

        // Create a native proxy according to XA data source
        DataSource ds = createDataSourceProxyXANative();
        // Create a global tx
        GlobalTransaction gtx = createGlobalTransaction();

        gtx.begin();
        runInGlobalTx(ds);
        gtx.rollback();

        Thread.sleep(5000);
    }

    private void runInGlobalTx(DataSource ds) throws SQLException {
        System.out.println(RootContext.getXID());

        Connection testConn = ds.getConnection();
        Statement testStat = testConn.createStatement();
        // >>> insert the test record with XA mode
        testStat.execute("insert into test(id, name) values(" + TEST_RECORD_ID + ", '" + TEST_RECORD_NAME + "')");
        // >>> close the statement and connection
        testStat.close();
        testConn.close();
    }

    private DataSourceProxyXANative createDataSourceProxyXANative() throws Throwable {
        XADataSource originalDS = createNewDruidXADataSource();
        DataSourceProxyXANative dataSourceProxyXA = new DataSourceProxyXANative(originalDS);
        return dataSourceProxyXA;
    }

    private DataSourceProxyXA createDataSourceProxyXA() throws Throwable {
        DataSource originalDS = createNewDruidDataSource();
        DataSourceProxyXA dataSourceProxyXA = new DataSourceProxyXA(originalDS);
        return dataSourceProxyXA;
    }

    private GlobalTransaction createGlobalTransaction() {
        String vgroup = "default_tx_group";
        GlobalTransactionScanner scanner = new GlobalTransactionScanner(vgroup);
        scanner.afterPropertiesSet();

        GlobalTransaction gtx = GlobalTransactionContext.getCurrentOrCreate();
        return gtx;
    }
}
