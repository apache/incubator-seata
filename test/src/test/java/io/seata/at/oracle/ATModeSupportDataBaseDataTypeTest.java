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
package io.seata.at.oracle;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import com.alibaba.druid.util.JdbcConstants;
import io.seata.rm.datasource.DataCompareUtils;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import io.seata.rm.datasource.sql.struct.TableRecords;
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

    /**
     * test1:
     * url:jdbc:oracle:thin:@localhost:49161:xe
     * name:system
     * password:oracle
     *
     * test2:
     * jdbc:oracle:thin:@localhost:1521:helowin
     * name:system
     * password:helowin
     */
    private static final String ORACLE_JDBC_URL = "jdbc:oracle:thin:@localhost:49161:xe";
    private static final String ORACLE_USERNAME = "system";
    private static final String ORACLE_PASSWORD = "oracle";
    private static final String ORACLE_DRIVER_CLASSNAME = JdbcUtils.ORACLE_DRIVER;

    private static final int NUMBER_TYPE = 1;

    /**
     * test number type tableName
     */
    private static final String NUMBER_TABLE_NAME = "T_DATA_TYPE_NUMBER_TEST";
    /**
     * test number type insert sql
     */
    private static final String TEST_NUMBER_TYPE_INSERT_SQL = "INSERT INTO T_DATA_TYPE_NUMBER_TEST\n"
        + "    (id,TINYINT_TEST,MEDUIMINT_TEST,INT_TEST,BIGINT_TEST,SMALLINT_TEST,INTEGER_TEST,DECIMAL_TEST,NUMERIC_TEST,DEC_TEST)\n"
        + "    VALUES (1,123,456,55,234,56,22423,45645.22,897.333,999)";
    /**
     * test number type update sql
     */
    private static final String TEST_NUMBER_TYPE_UPDATE_SQL = "UPDATE T_DATA_TYPE_NUMBER_TEST\n"
        + "set TINYINT_TEST = 312,MEDUIMINT_TEST = 654,INT_TEST = 55,BIGINT_TEST = 432,\n"
        + "    SMALLINT_TEST = 65,INTEGER_TEST = 32422,DECIMAL_TEST = 54654.22,NUMERIC_TEST = 798.333,\n"
        + "    DEC_TEST = 888 WHERE id =" + TEST_RECORD_ID;

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
    public void doHandlerTest() throws Throwable {
        doType(1, false);
        // doType(2,false);
        // doType(3,false);
        // doType(4,false);
    }

    public void doType(int type, boolean globalCommit) throws Throwable {
        String tableName = "";
        String updateSql = "";
        String insertSql = "";
        switch (type) {
            case 1:
                LOGGER.info("current type is:[{}]", type);
                insertSql = TEST_NUMBER_TYPE_INSERT_SQL;
                tableName = NUMBER_TABLE_NAME;
                updateSql = TEST_NUMBER_TYPE_UPDATE_SQL;
                break;
            case 2:

                break;
            default:

        }

        testTypeSql(globalCommit, insertSql, tableName, updateSql);
    }

    @Test
    @Disabled
    public void testTypeSql(boolean globalCommit, String insertSql, String tableName, String updateSql)
        throws Throwable {
        doExecute(insertSql);
        doTestOracleTypePhase(globalCommit, tableName, updateSql);
    }


    private void doExecute(String prepareSql) throws Throwable{
        DruidDataSource oracleDruidDataSource = createOracleDruidDataSource();
        Connection helperConn = oracleDruidDataSource.getConnection();
        Statement helperStat = helperConn.createStatement();
        helperStat.execute(prepareSql);
        helperStat.close();
        helperConn.close();
        LOGGER.info("insert sql success sql:[{}]", prepareSql);
    }

    private void doTestOracleTypePhase(boolean globalCommit, String tableName, String updateSql) throws Throwable {
        // init DataSource: helper
        DruidDataSource helperDS = createOracleDruidDataSource();

        Connection helperConn = null;
        Statement helperStat = null;
        ResultSet helperRes = null;

        initRM();

        final DataSourceProxy dataSourceProxy = new DataSourceProxy(helperDS);
        LOGGER.info("the dataSourceProxy is:[{}]", dataSourceProxy);
        RootContext.bind(MOCK_XID);
        Connection testConn = dataSourceProxy.getConnection();
        LOGGER.info("the testConn is:[{}]", testConn);
        Statement testStat = testConn.createStatement();
        LOGGER.info("the testStat is:[{}]", testStat);

        // get before image
        helperConn = helperDS.getConnection();
        helperStat = helperConn.createStatement();
        helperRes = helperStat.executeQuery("select * from " + tableName + " where id = " + TEST_RECORD_ID);
        LOGGER.info("the helperRes is:[{}]", helperRes);
        TableMeta tableMeta = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.ORACLE.name())
            .getTableMeta(dataSourceProxy.getPlainConnection(), tableName, dataSourceProxy.getResourceId());
        TableRecords beforeImage = TableRecords.buildRecords(tableMeta, helperRes);

        // if not throw exception update record
        Assertions.assertDoesNotThrow(() -> testStat.execute(updateSql));

        // close
        testStat.close();
        testConn.close();
        RootContext.unbind();

        if (globalCommit) {
            Assertions
                .assertDoesNotThrow(() -> DefaultResourceManager.get().branchCommit(dataSourceProxy.getBranchType(),
                    MOCK_XID, MOCK_BRANCH_ID, dataSourceProxy.getResourceId(), null));
        } else {
            DefaultResourceManager.get().branchRollback(dataSourceProxy.getBranchType(), MOCK_XID, MOCK_BRANCH_ID,
                dataSourceProxy.getResourceId(), null);
            helperConn = helperDS.getConnection();
            helperStat = helperConn.createStatement();
            helperRes = helperStat.executeQuery("select * from " + tableName + " where id = " + TEST_RECORD_ID);
            TableRecords currentImage = TableRecords.buildRecords(tableMeta, helperRes);
            Assertions.assertTrue(DataCompareUtils.isRecordsEquals(beforeImage, currentImage).getResult());
            helperRes.close();
            helperStat.close();
            helperConn.close();
        }
    }

    private void initRM() {
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
