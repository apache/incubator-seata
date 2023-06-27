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
package io.seata.at.mysql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.util.JdbcUtils;
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
import io.seata.server.UUIDGenerator;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


/**
 * @author renliangyu857
 */
public class MysqlUpdateJoinTest {
    private static final int testRecordId = 1;
    private static final int testRecordId1 = 2;
    private static final long testTid = UUIDGenerator.generateUUID();
    private static final String mockXid = "127.0.0.1:8091:" + testTid;
    private static final long mockBranchId = testTid + 1;

    private static final String mysql_jdbcUrl = "jdbc:mysql://127.0.0.1:3306/seata";
    private static final String mysql_username = "demo";
    private static final String mysql_password = "demo";
    private static final String mysql_driverClassName = JdbcUtils.MYSQL_DRIVER;


    @Test
    @Disabled
    public void testUpdateJoin() throws Throwable {
        doTestPhase2(false, "update t inner join t1 on t.a = t1.a set b = 3,t.c=3");
        System.out.println("AT MODE Phase2 test for update join looks good!");
    }

    private static void doPrepareData(String prepareSql) throws Throwable {
        // init DataSource: helper
        DruidDataSource helperDS = createNewDruidDataSource();

        // prepare data for test: make sure no test record there
        Connection helperConn = helperDS.getConnection();
        Statement helperStat = helperConn.createStatement();
        helperStat.execute(prepareSql);
        helperStat.close();
        helperConn.close();
    }


    private void doTestPhase2(boolean globalCommit, String updateSql) throws Throwable {
        // init DataSource: helper
        DruidDataSource helperDS = createNewDruidDataSource();

        Connection helperConn = null;
        Statement helperStat = null;
        ResultSet table1HelperRes = null;
        ResultSet table2HelperRes = null;

        initRM();

        final DataSourceProxy dataSourceProxy = new DataSourceProxy(createNewDruidDataSource());

        RootContext.bind(mockXid);
        Connection testConn = dataSourceProxy.getConnection();
        Statement testStat = testConn.createStatement();

        // >>> query before image
        helperConn = helperDS.getConnection();
        helperStat = helperConn.createStatement();
        table1HelperRes = helperStat.executeQuery("select * from t where id = " + testRecordId );
        TableMeta table1Meta = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.MYSQL).getTableMeta(dataSourceProxy.getPlainConnection(),
                "t", dataSourceProxy.getResourceId());
        TableRecords table1BeforeImage = TableRecords.buildRecords(table1Meta, table1HelperRes);
        table2HelperRes = helperStat.executeQuery("select * from t1 where id = " + testRecordId1);
        TableMeta table2Meta = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.MYSQL).getTableMeta(dataSourceProxy.getPlainConnection(),
                "t1", dataSourceProxy.getResourceId());
        TableRecords table2BeforeImage = TableRecords.buildRecords(table2Meta, table2HelperRes);
        // >>> update record should not throw exception
        Assertions.assertDoesNotThrow(() -> testStat.execute(updateSql));
        // >>> close the statement and connection
        testStat.close();
        testConn.close();
        RootContext.unbind();

        if (globalCommit) {
            // >>> Global Tx Phase 2: commit should not throw exception
            Assertions.assertDoesNotThrow(() -> DefaultResourceManager.get().branchCommit(dataSourceProxy.getBranchType(), mockXid, mockBranchId,
                    dataSourceProxy.getResourceId(), null));
        } else {
            DefaultResourceManager.get().branchRollback(dataSourceProxy.getBranchType(), mockXid, mockBranchId, dataSourceProxy.getResourceId(), null);
            // >>> Global Tx Phase 2: rollback have a check,rollbacked record must equal to before image
            helperConn = helperDS.getConnection();
            helperStat = helperConn.createStatement();
            table1HelperRes = helperStat.executeQuery("select * from t where id = " + testRecordId);
            TableRecords table1CurrentImage = TableRecords.buildRecords(table1Meta, table1HelperRes);
            table2HelperRes = helperStat.executeQuery("select * from t1 where id = " + testRecordId1);
            TableRecords table2CurrentImage = TableRecords.buildRecords(table2Meta, table2HelperRes);
            Assertions.assertTrue(DataCompareUtils.isRecordsEquals(table1BeforeImage, table1CurrentImage).getResult());
            Assertions.assertTrue(DataCompareUtils.isRecordsEquals(table2BeforeImage, table2CurrentImage).getResult());
            table1HelperRes.close();
            table2HelperRes.close();
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
                return mockBranchId;
            }

            @Override
            public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status, String applicationData) throws TransactionException {
            }
        });

    }

    private static DruidDataSource createNewDruidDataSource() throws Throwable {
        DruidDataSource druidDataSource = new DruidDataSource();
        initDruidDataSource(druidDataSource);
        return druidDataSource;
    }

    private static void initDruidDataSource(DruidDataSource druidDataSource) throws Throwable {
        druidDataSource.setDbType(JdbcConstants.MYSQL);
        druidDataSource.setUrl(mysql_jdbcUrl);
        druidDataSource.setUsername(mysql_username);
        druidDataSource.setPassword(mysql_password);
        druidDataSource.setDriverClassName(mysql_driverClassName);
        druidDataSource.init();
    }
}