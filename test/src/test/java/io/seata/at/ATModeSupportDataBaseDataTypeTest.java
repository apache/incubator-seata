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
import io.seata.common.exception.NotSupportYetException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.druid.pool.DruidDataSource;
import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.datasource.DataCompareUtils;
import io.seata.rm.datasource.DataSourceManager;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.sqlparser.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import io.seata.rm.datasource.sql.struct.TableRecords;
import static io.seata.at.DruidDataSourceUtils.ORACLE;
import static io.seata.at.DruidDataSourceUtils.POSTGRESQL;
import static io.seata.at.DruidDataSourceUtils.createNewDruidDataSource;
import static io.seata.at.oracle.OracleSqlConstant.BINARY_TABLE_NAME;
import static io.seata.at.oracle.OracleSqlConstant.BINARY_TYPE;
import static io.seata.at.oracle.OracleSqlConstant.DATE_TABLE_NAME;
import static io.seata.at.oracle.OracleSqlConstant.DATE_TYPE;
import static io.seata.at.oracle.OracleSqlConstant.NUMBER_TABLE_NAME;
import static io.seata.at.oracle.OracleSqlConstant.NUMBER_TYPE;
import static io.seata.at.oracle.OracleSqlConstant.STRING_TABLE_NAME;
import static io.seata.at.oracle.OracleSqlConstant.STRING_TYPE;
import static io.seata.at.oracle.OracleSqlConstant.TEST_BINARY_TYPE_INSERT_SQL;
import static io.seata.at.oracle.OracleSqlConstant.TEST_BINARY_TYPE_UPDATE_SQL;
import static io.seata.at.oracle.OracleSqlConstant.TEST_DATE_TYPE_INSERT_SQL;
import static io.seata.at.oracle.OracleSqlConstant.TEST_DATE_TYPE_UPDATE_SQL;
import static io.seata.at.oracle.OracleSqlConstant.TEST_NUMBER_TYPE_INSERT_SQL;
import static io.seata.at.oracle.OracleSqlConstant.TEST_NUMBER_TYPE_UPDATE_SQL;
import static io.seata.at.oracle.OracleSqlConstant.TEST_RECORD_ID;
import static io.seata.at.oracle.OracleSqlConstant.TEST_STRING_TYPE_INSERT_SQL;
import static io.seata.at.oracle.OracleSqlConstant.TEST_STRING_TYPE_UPDATE_SQL;

/**
 * add AT transaction mode tests to support database data types (Oracle)
 *
 * author doubleDimple
 */
@Disabled
public class ATModeSupportDataBaseDataTypeTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ATModeSupportDataBaseDataTypeTest.class);

    private static final long TEST_ID = 923123123123123132L;
    private static final String MOCK_XID = "127.0.0.1:8091:" + TEST_ID;
    private static final long MOCK_BRANCH_ID = TEST_ID + 1;

    @Test
    public void doSqlTest() throws Throwable {
        doHandlerTest(ORACLE);
    }

    public void doHandlerTest(int sqlType) throws Throwable {
        doType(sqlType, NUMBER_TYPE, false);
        doType(sqlType, STRING_TYPE, false);
        doType(sqlType, DATE_TYPE, false);
        doType(sqlType, BINARY_TYPE, false);
    }

    public void doType(int sqlType, int type, boolean globalCommit) throws Throwable {
        final SqlClass sqlClass = dogetType(sqlType, type);
        LOGGER.info("current type is:[{}]", type);
        testTypeSql(sqlType, globalCommit, sqlClass.getInsertSql(), sqlClass.getTableName(), sqlClass.getUpdateSql());
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
        helperRes = helperStat.executeQuery("select * from " + tableName + " where id = " + TEST_RECORD_ID);
        LOGGER.info("the helperRes is:[{}]", helperRes);
        TableMeta tableMeta = TableMetaCacheFactory.getTableMetaCache(io.seata.sqlparser.util.JdbcConstants.ORACLE)
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

    private static class SqlClass {

        private String tableName = "";
        private String updateSql = "";
        private String insertSql = "";

        public SqlClass() {}

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getUpdateSql() {
            return updateSql;
        }

        public void setUpdateSql(String updateSql) {
            this.updateSql = updateSql;
        }

        public String getInsertSql() {
            return insertSql;
        }

        public void setInsertSql(String insertSql) {
            this.insertSql = insertSql;
        }
    }

    private SqlClass dogetType(int sqlType, int type) {
        SqlClass sqlClass = new SqlClass();
        switch (sqlType) {
            case ORACLE:
                switch (type) {
                    case 1:
                        sqlClass.setInsertSql(TEST_NUMBER_TYPE_INSERT_SQL);
                        sqlClass.setTableName(NUMBER_TABLE_NAME);
                        sqlClass.setUpdateSql(TEST_NUMBER_TYPE_UPDATE_SQL);
                        break;
                    case 2:
                        sqlClass.setInsertSql(TEST_STRING_TYPE_INSERT_SQL);
                        sqlClass.setTableName(STRING_TABLE_NAME);
                        sqlClass.setUpdateSql(TEST_STRING_TYPE_UPDATE_SQL);
                        break;
                    case 3:
                        sqlClass.setInsertSql(TEST_DATE_TYPE_INSERT_SQL);
                        sqlClass.setTableName(DATE_TABLE_NAME);
                        sqlClass.setTableName(TEST_DATE_TYPE_UPDATE_SQL);
                        break;
                    case 4:
                        sqlClass.setInsertSql(TEST_BINARY_TYPE_INSERT_SQL);
                        sqlClass.setTableName(BINARY_TABLE_NAME);
                        sqlClass.setTableName(TEST_BINARY_TYPE_UPDATE_SQL);
                    default:
                }
                break;
            case POSTGRESQL:

                break;
            default:
                throw new NotSupportYetException("not support");
        }
        return sqlClass;
    }
}
