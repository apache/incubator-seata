package io.seata.at;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.util.JdbcUtils;
import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.datasource.DataSourceManager;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.server.UUIDGenerator;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ATDbTypeTest {
    private static final int testRecordId = 1;
    private static final long testTid = UUIDGenerator.generateUUID();
    private static final String mockXid = "127.0.0.1:8091:" + testTid;
    private static final long mockBranchId = testTid + 1;

    private static final String dbType = JdbcConstants.MYSQL;
    private static final boolean mySQL8 = true;
    private static final String pg_jdbcUrl = "jdbc:postgresql://127.0.0.1:5432/postgres";
    private static final String pg_username = "postgres";
    private static final String pg_password = "postgres";
    private static final String pg_driverClassName = JdbcUtils.POSTGRESQL_DRIVER;

    private static final String mysql_jdbcUrl = "jdbc:mysql://127.0.0.1:3306/demo";
    private static final String mysql_username = "demo";
    private static final String mysql_password = "demo";
    private static final String mysql_driverClassName = JdbcUtils.MYSQL_DRIVER;

    private static final String mysql8_jdbcUrl = "jdbc:mysql://127.0.0.1:3306/demo?useUnicode=true&characterEncoding=utf-8&useSSL=false";
    private static final String mysql8_username = "demo";
    private static final String mysql8_password = "demo";
    private static final String mysql8_driverClassName = JdbcUtils.MYSQL_DRIVER_6;

    private static final String oracle_jdbcUrl = "jdbc:oracle:thin:@localhost:1521:xe";
    private static final String oracle_username = "demo";
    private static final String oracle_password = "demo";
    private static final String oracle_driverClassName = JdbcUtils.ORACLE_DRIVER;

    @Test
    public void testAtModeNumericTypes() throws Throwable {
        doPrepareData("insert into test_db_type(id,tinyint_type,smallint_type,mediumint_type,int_type,bigint_type,float_type,double_type,decimal_type,numeric_type) values(1,1,1,1,1,1,1.2,1.2,1.2,1.2)");
        doTestAtNumericTypePhase2(true,mockXid,mockBranchId);
    }


    private void doPrepareData(String prepareSql) throws Throwable{
        // init DataSource: helper
        DruidDataSource helperDS = createNewDruidDataSource();

        // prepare data for test: make sure no test record there
        Connection helperConn = helperDS.getConnection();
        Statement helperStat = helperConn.createStatement();
        helperStat.execute(prepareSql);
        helperStat.close();
        helperConn.close();

    }

    private void doTestAtNumericTypePhase2(Boolean globalCommit,String mockXid, Long mockBranchId) throws Throwable {
        // init DataSource: helper
        DruidDataSource helperDS = createNewDruidDataSource();

        Connection helperConn = null;
        Statement helperStat = null;
        ResultSet helperRes = null;

        // init RM
        initRM();

        DataSourceProxy dataSourceProxy = null;
        // init DataSource: runner
        DruidDataSource runnerDS = createNewDruidDataSource();
        dataSourceProxy = new DataSourceProxy(runnerDS);

        // Global Tx Phase 1:
        RootContext.bind(mockXid);
        Connection testConn = dataSourceProxy.getConnection();
        Statement testStat = testConn.createStatement();
        // >>> update the test record with AT mode
        testStat.execute("update test_db_type set tinyint_type = 2,smallint_type = 2,mediumint_type = 2,int_type = 2,bigint_type = 2,float_type = 2.2,double_type = 2.2,decimal_type = 2.2, numeric_type = 2.2 where id = " + testRecordId);
        // >>> close the statement and connection
        testStat.close();
        testConn.close();
        RootContext.unbind();

        // Global Tx Phase 2: run phase 2 with the same runner DS
        if(globalCommit) {
            DefaultResourceManager.get().branchCommit(dataSourceProxy.getBranchType(), mockXid, mockBranchId,
                    dataSourceProxy.getResourceId(), null);
            // have a check
            helperConn = helperDS.getConnection();
            helperStat = helperConn.createStatement();
            helperRes = helperStat.executeQuery("select * from test_db_type where id = " + testRecordId);
            // should see the test record now
            Assertions.assertTrue(helperRes.next());
            Assertions.assertEquals(helperRes.getInt(2),2);
            Assertions.assertEquals(helperRes.getInt(3),2);
            Assertions.assertEquals(helperRes.getInt(4),2);
            Assertions.assertEquals(helperRes.getInt(5),2);
            Assertions.assertEquals(helperRes.getInt(6),2);
            Assertions.assertEquals(helperRes.getFloat(7),2.2,0.01);
            Assertions.assertEquals(helperRes.getDouble(8),2.2,0.01);
            Assertions.assertEquals(helperRes.getBigDecimal(9).floatValue(),2.20,0.01);
            Assertions.assertEquals(helperRes.getBigDecimal(10).floatValue(),2.20,0.01);
            helperRes.close();
            helperStat.close();
            helperConn.close();
        }else {
            DefaultResourceManager.get().branchRollback(dataSourceProxy.getBranchType(), mockXid, mockBranchId, dataSourceProxy.getResourceId(), null);
             //have a check
            helperConn = helperDS.getConnection();
            helperStat = helperConn.createStatement();
            helperRes = helperStat.executeQuery("select * from test where id = " + testRecordId);
            // should see the rollbacked test record now
            Assertions.assertTrue(helperRes.next());
            Assertions.assertEquals(helperRes.getInt(1), 1);
            Assertions.assertEquals(helperRes.getInt(2),1);
            Assertions.assertEquals(helperRes.getInt(3),1);
            Assertions.assertEquals(helperRes.getInt(4),1);
            Assertions.assertEquals(helperRes.getInt(5),1);
            Assertions.assertEquals(helperRes.getFloat(6),1.2,0.01);
            Assertions.assertEquals(helperRes.getDouble(7),1.2,0.01);
            Assertions.assertEquals(helperRes.getBigDecimal(8).floatValue(),1.2,0.01);
            Assertions.assertEquals(helperRes.getBigDecimal(9).floatValue(),1.2,0.01);
            helperRes.close();
            helperStat.close();
            helperConn.close();
        }
        System.out.println("AT MODE phase2 test for numeric db type looks good!");
    }

    private void initRM() throws Throwable {
        // init RM
        DefaultResourceManager.get();
        // mock the RM of AT
        DefaultResourceManager.mockResourceManager(BranchType.AT, new DataSourceManager() {
            @Override
            public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid, String applicationData, String lockKeys) throws TransactionException {
                return mockBranchId;
            }

            @Override
            public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status, String applicationData) throws TransactionException {
            }
        });

    }

    private DruidDataSource createNewDruidDataSource() throws Throwable {
        DruidDataSource druidDataSource = new DruidDataSource();
        initDruidDataSource(druidDataSource);
        return druidDataSource;
    }

    private void initDruidDataSource(DruidDataSource druidDataSource) throws Throwable {
        druidDataSource.setDbType(dbType);
        if (dbType.equalsIgnoreCase(JdbcConstants.POSTGRESQL)) {
            druidDataSource.setUrl(pg_jdbcUrl);
            druidDataSource.setUsername(pg_username);
            druidDataSource.setPassword(pg_password);
            druidDataSource.setDriverClassName(pg_driverClassName);

        } else if (dbType.equalsIgnoreCase(JdbcConstants.MYSQL)) {
            if (mySQL8) {
                druidDataSource.setUrl(mysql8_jdbcUrl);
                druidDataSource.setUsername(mysql8_username);
                druidDataSource.setPassword(mysql8_password);
                druidDataSource.setDriverClassName(mysql8_driverClassName);

            } else {
                druidDataSource.setUrl(mysql_jdbcUrl);
                druidDataSource.setUsername(mysql_username);
                druidDataSource.setPassword(mysql_password);
                druidDataSource.setDriverClassName(mysql_driverClassName);
            }

        } else if (dbType.equalsIgnoreCase(JdbcConstants.ORACLE)) {
            druidDataSource.setUrl(oracle_jdbcUrl);
            druidDataSource.setUsername(oracle_username);
            druidDataSource.setPassword(oracle_password);
            druidDataSource.setDriverClassName(oracle_driverClassName);

        } else {
            throw new IllegalAccessError("Unknown dbType: " + dbType);
        }
        druidDataSource.init();
    }
}
