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
package io.seata.at;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * add AT transaction mode tests to support database data types (Oracle)
 *
 * author doubleDimple
 */
@Disabled
public class ATModeSupportDataBaseDataTypeTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ATModeSupportDataBaseDataTypeTest.class);

    private static final int TEST_RECORD_ID = 1;
    private static final long TEST_ID = UUIDGenerator.generateUUID();
    private static final String MOCK_XID = "127.0.0.1:8091:" + TEST_ID;
    private static final long MOCK_BRANCH_ID = TEST_ID + 1;

    private static final String ORACLE_JDBC_URL = "jdbc:oracle:thin:@localhost:1521:helowin";
    private static final String ORACLE_USERNAME = "system";
    private static final String ORACLE_PASSWORD = "helowin";
    private static final String ORACLE_DRIVER_CLASSNAME = JdbcUtils.ORACLE_DRIVER;

    /**
     * test char type
     */
    private static final String TEST_CHAR_TYPE_SQL = "";
    /**
     * test varchar type
     */
    private static final String TEST_VARCHAR_TYPE_SQL = "";
    /**
     * test varchar2 type
     */
    private static final String TEST_VARCHAR2_TYPE_SQL = "";
    /**
     * test number type
     */
    private static final String TEST_NUMBER_TYPE_SQL = "";
    /**
     * test clob type
     */
    private static final String TEST_CLOB_TYPE_SQL = "";
    /**
     * test blob type
     */
    private static final String TEST_BLOB_TYPE_SQL = "";

    @Test
    @Disabled
    public void testDruidDataSource() {
        try {
            DruidDataSource oracleDruidDataSource = createOracleDruidDataSource();
            LOGGER.info("get datasource success,the dataSource is:[{}]", oracleDruidDataSource.toString());
        } catch (Throwable throwable) {
            LOGGER.error("the reason: [{}]",throwable.getMessage());
        }
    }

    @Test
    @Disabled
    public void testCharTypeSql() throws Throwable {
        doExecute(TEST_CHAR_TYPE_SQL);
        doTestCharTypePhase(true,MOCK_XID,MOCK_BRANCH_ID);
    }


    private void doExecute(String prepareSql) throws Throwable{
        DruidDataSource oracleDruidDataSource = createOracleDruidDataSource();
        Connection helperConn = oracleDruidDataSource.getConnection();
        Statement helperStat = helperConn.createStatement();
        helperStat.execute(prepareSql);
        helperStat.close();
        helperConn.close();
    }

    private void doTestCharTypePhase(Boolean globalCommit,String mockXid, Long mockBranchId) throws Throwable {
        // init DataSource: helper
        DruidDataSource helperDS = createOracleDruidDataSource();

        Connection helperConn = null;
        Statement helperStat = null;
        ResultSet helperRes = null;

        // init RM
        initRM();

        DataSourceProxy dataSourceProxy = null;
        // init DataSource: runner
        DruidDataSource runnerDS = createOracleDruidDataSource();
        dataSourceProxy = new DataSourceProxy(runnerDS);

        // Global Tx Phase 1:
        RootContext.bind(mockXid);
        Connection testConn = dataSourceProxy.getConnection();
        Statement testStat = testConn.createStatement();
        // >>> update the test record with AT mode
        testStat.execute("update test set name = '123' where id = " + TEST_RECORD_ID);
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
            helperRes = helperStat.executeQuery("select * from test where id = " + TEST_RECORD_ID);
            // should see the test record now
            Assertions.assertTrue(helperRes.next());
            helperRes.close();
            helperStat.close();
            helperConn.close();
        }else {
            DefaultResourceManager.get().branchRollback(dataSourceProxy.getBranchType(), mockXid, mockBranchId, dataSourceProxy.getResourceId(), null);
            //have a check
            helperConn = helperDS.getConnection();
            helperStat = helperConn.createStatement();
            helperRes = helperStat.executeQuery("select * from test where id = " + TEST_RECORD_ID);
            // should see the rollbacked test record now
            Assertions.assertTrue(helperRes.next());
            helperRes.close();
            helperStat.close();
            helperConn.close();
        }
        System.out.println("Phase2 looks good!!");
    }

    private void initRM() throws Throwable {
        // init RM
        DefaultResourceManager.get();
        // mock the RM of AT
        DefaultResourceManager.mockResourceManager(BranchType.AT, new DataSourceManager() {
            @Override
            public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid, String applicationData, String lockKeys) throws TransactionException {
                return MOCK_BRANCH_ID;
            }

            @Override
            public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status, String applicationData) throws TransactionException {
            }
        });

    }

    private DruidDataSource createOracleDruidDataSource() throws Throwable {
        DruidDataSource druidOracleDataSource = new DruidDataSource();
        druidOracleDataSource.setUrl(ORACLE_JDBC_URL);
        druidOracleDataSource.setUsername(ORACLE_USERNAME);
        druidOracleDataSource.setPassword(ORACLE_PASSWORD);
        druidOracleDataSource.setDriverClassName(ORACLE_DRIVER_CLASSNAME);
        druidOracleDataSource.init();
        LOGGER.info("datasource init success");
        return druidOracleDataSource;
    }
}
