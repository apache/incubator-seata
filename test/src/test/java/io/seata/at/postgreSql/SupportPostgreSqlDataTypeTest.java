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
package io.seata.at.postgreSql;

import com.alibaba.druid.pool.DruidDataSource;
import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.datasource.DataCompareUtils;
import io.seata.rm.datasource.DataSourceManager;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import static io.seata.at.DruidDataSourceUtils.POSTGRESQL;
import static io.seata.at.DruidDataSourceUtils.createNewDruidDataSource;
import static io.seata.at.oracle.OracleSqlConstant.TEST_RECORD_ID;
import static io.seata.at.postgreSql.PostgreSqlConstant.CURRENCY_TABLE_NAME;
import static io.seata.at.postgreSql.PostgreSqlConstant.CURRENCY_TYPE;
import static io.seata.at.postgreSql.PostgreSqlConstant.NUMBER_TYPE;
import static io.seata.at.postgreSql.PostgreSqlConstant.TEST_CURRENCY_TYPE_INSERT_SQL;
import static io.seata.at.postgreSql.PostgreSqlConstant.TEST_CURRENCY_TYPE_UPDATE_SQL;

/**
 * add AT transaction mode tests to support database data types (Oracle)
 *
 * author doubleDimple
 */
@Disabled
public class SupportPostgreSqlDataTypeTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupportPostgreSqlDataTypeTest.class);

    private static final long TEST_ID = 923123123123123132L;
    private static final String MOCK_XID = "127.0.0.1:8091:" + TEST_ID;
    private static final long MOCK_BRANCH_ID = TEST_ID + 1;

    @Test
    public void doHandlerTest() throws Throwable {
        doType(CURRENCY_TYPE, false);
        doType(NUMBER_TYPE, false);
    }

    public void doType(int type, boolean globalCommit) throws Throwable {
        String insertSql = "";
        String tableName = "";
        String updateSql = "";
        switch (type) {
            case 1:
                insertSql = TEST_CURRENCY_TYPE_INSERT_SQL;
                tableName = CURRENCY_TABLE_NAME;
                updateSql = TEST_CURRENCY_TYPE_UPDATE_SQL;
                break;
            default:
        }
        LOGGER.info("current type is:[{}]", type);
        testTypeSql(POSTGRESQL, globalCommit, insertSql, tableName, updateSql);
    }

    @Test
    public void testTypeSql(int sqlType, boolean globalCommit, String insertSql, String tableName, String updateSql)
        throws Throwable {
        doExecute(sqlType, insertSql);
        doTestOracleTypePhase(sqlType, globalCommit, tableName, updateSql);
    }

    private void doExecute(int sqlType, String prepareSql) throws Throwable {
        DruidDataSource oracleDruidDataSource = createNewDruidDataSource(sqlType);
        Connection helperConn = oracleDruidDataSource.getConnection();
        Statement helperStat = helperConn.createStatement();
        helperStat.execute(prepareSql);
        helperStat.close();
        helperConn.close();
        LOGGER.info("insert sql success sql:[{}]", prepareSql);
    }

    private void doTestOracleTypePhase(int sqlType, boolean globalCommit, String tableName, String updateSql)
        throws Throwable {
        // init DataSource: helper
        DruidDataSource helperDS = createNewDruidDataSource(sqlType);

        Connection helperConn = null;
        Statement helperStat = null;
        ResultSet helperRes = null;

        initRM();

        DataSourceProxy dataSourceProxy = new DataSourceProxy(createNewDruidDataSource(sqlType));
        LOGGER.info("the dataSourceProxy is:[{}]", dataSourceProxy);
        RootContext.bind(MOCK_XID);
        Connection testConn = dataSourceProxy.getConnection();
        LOGGER.info("the testConn is:[{}]", testConn);
        Statement testStat = testConn.createStatement();
        LOGGER.info("the testStat is:[{}]", testStat);

        // get before image
        helperConn = helperDS.getConnection();
        helperStat = helperConn.createStatement();
        helperRes = helperStat.executeQuery("select * from " + tableName + " where \"ID\" = " + TEST_RECORD_ID);
        LOGGER.info("the helperRes is:[{}]", helperRes);
        TableMeta tableMeta = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.POSTGRESQL)
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
            helperRes = helperStat.executeQuery("select * from " + tableName + " where \"ID\" = " + TEST_RECORD_ID);
            TableRecords currentImage = TableRecords.buildRecords(tableMeta, helperRes);
            LOGGER.info("the currentImage Rows is:[{}]", currentImage.getRows());
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
            public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid,
                String applicationData, String lockKeys) throws TransactionException {
                return MOCK_BRANCH_ID;
            }

            @Override
            public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status,
                String applicationData) throws TransactionException {}
        });

    }
}
