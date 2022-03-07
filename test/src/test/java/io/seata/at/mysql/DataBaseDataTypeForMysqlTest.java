package io.seata.at.mysql;

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
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.server.UUIDGenerator;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

public class DataBaseDataTypeForMysqlTest {
    private static final int testRecordId = 1;
    private static final long testTid = UUIDGenerator.generateUUID();
    private static final String mockXid = "127.0.0.1:8091:" + testTid;
    private static final long mockBranchId = testTid + 1;

    private static final boolean mySQL8 = true;

    private static final String mysql_jdbcUrl = "jdbc:mysql://10.82.30.93:3306/demo";
    private static final String mysql_username = "root";
    private static final String mysql_password = "f2edfsfeKJHdssku";
    private static final String mysql_driverClassName = JdbcUtils.MYSQL_DRIVER;

    private static final String mysql8_jdbcUrl = "jdbc:mysql://10.82.12.118:3306/brms_xrule_uata?useUnicode=true&characterEncoding=utf-8&useSSL=false";
    private static final String mysql8_username = "code_xrule";
    private static final String mysql8_password = "Xrule@1234";
    private static final String mysql8_driverClassName = JdbcUtils.MYSQL_DRIVER_6;

    private static final Integer INTEGER_TYPE = 1;
    private static final Integer DATE_TYPE = 2;
    private static final Integer STRING_TYPE = 3;
    private static final Integer POINT_TYPE = 4;
    private static final Integer SPECIAL_TYPE = 5;


    @Test
    public void testNumericTypes() throws Throwable {
        doPrepareData("insert into t_test_numeric_type(id,tinyint_type,smallint_type,mediumint_type,int_type,bigint_type,float_type,double_type,decimal_type,numeric_type) values(1,1,1,1,1,1,1.2,1.2,1.2,1.2)");
        doTestDbTypePhase2(INTEGER_TYPE, false, "t_test_numeric_type", "update t_test_numeric_type set tinyint_type = 2,smallint_type = 2,mediumint_type = 2,int_type = 2,bigint_type = 2,float_type = 2.2,double_type = 2.2,decimal_type = 2.2, numeric_type = 2.2 where id = " + testRecordId);
        System.out.println("AT MODE Phase2 test for numeric db type looks good!");
    }

    @Test
    public void testDateTypes() throws Throwable {
        doPrepareData("insert into t_test_date_type(id,date_type,time_type,year_type,datetime_type,timestamp_type) values(1,now(),now(),now(),now(),now())");
        TimeUnit.SECONDS.sleep(2);
        doTestDbTypePhase2(DATE_TYPE, false, "t_test_date_type", "update t_test_date_type set date_type=date_add(now(),INTERVAL 1 day),time_type=now(),year_type=date_add(now(),INTERVAL 1 YEAR),datetime_type=now(),timestamp_type=now()");
        System.out.println("AT MODE Phase2 test for date db type looks good!");
    }

    @Test
    public void testStringTypes() throws Throwable {
        doPrepareStringTypeData();
        doTestDbTypePhase2(STRING_TYPE, false, "t_test_string_type", "");
        System.out.println("AT MODE Phase2 test for string db type looks good!");
    }

    @Test
    public void testPointTypes() throws Throwable {
        doPrepareData("insert into t_test_point_type(id,point_type,linestring_type,polygon_type,geometry_type,multipoint_type,multi_linestring_type,multi_polygon_type,geometry_collection_type) \n" +
                "values(2,ST_GeomFromText('POINT(15 20)'),ST_GeomFromText('LINESTRING(0 0, 10 10, 20 25, 50 60)'),ST_GeomFromText('POLYGON((0 0,10 0,10 10,0 10,0 0),(5 5,7 5,7 7,5 7, 5 5))'),ST_GeomFromText('POINT(15 20)'),ST_GeomFromText('MULTIPOINT(0 0, 20 20, 60 60)'),ST_GeomFromText('MULTILINESTRING((10 10, 20 20), (15 15, 30 15))'),ST_GeomFromText('MULTIPOLYGON(((0 0,10 0,10 10,0 10,0 0)),((5 5,7 5,7 7,5 7, 5 5)))'),ST_GeomFromText('GEOMETRYCOLLECTION(POINT(1 -1), POINT(10 10), POINT(30 30), LINESTRING(15 15, 20 20))'))");
        doTestDbTypePhase2(POINT_TYPE, false, "t_test_point_type", "update t_test_point_type set point_type = ST_GeomFromText('POINT(10 20)'),\n" +
                "linestring_type = ST_GeomFromText('LINESTRING(0 0, 1 1, 20 25, 50 60)'),\n" +
                "polygon_type = ST_GeomFromText('POLYGON((0 0,1 0,10 10,0 10,0 0),(5 5,7 5,7 7,5 7, 5 5))'),\n" +
                "geometry_type = ST_GeomFromText('POINT(16 20)'),\n" +
                "multipoint_type = ST_GeomFromText('MULTIPOINT(0 0, 21 20, 60 60)'),\n" +
                "multi_linestring_type = ST_GeomFromText('MULTILINESTRING((11 10, 20 20), (15 15, 30 15))'),\n" +
                "multi_polygon_type = ST_GeomFromText('MULTIPOLYGON(((0 0,11 0,10 10,0 10,0 0)),((5 5,7 5,7 7,5 7, 5 5)))'),\n" +
                "geometry_collection_type = ST_GeomFromText('GEOMETRYCOLLECTION(POINT(1 -1), POINT(11 10), POINT(30 30), LINESTRING(15 15, 20 20))') where id = " + testRecordId);
        System.out.println("AT MODE Phase2 test for point db type looks good!");
    }

    @Test
    public void testSpecialTypes() throws Throwable {
        doPrepareData("insert into t_test_special_type(id,bit_type,real_type,binary_type,varbinary_type,enum_type,set_type) values(1,B'10000',1.1,'a','a','1','a')");
        doTestDbTypePhase2(SPECIAL_TYPE, false, "t_test_special_type", "update t_test_special_type set bit_type = B'10001',real_type = 1.2,binary_type = 'b',varbinary_type = 'b',enum_type = '2',set_type = 'b'");
        System.out.println("AT MODE Phase2 test for special db type looks good!");
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

    private void doPrepareStringTypeData() throws Throwable {
        DruidDataSource helperDS = createNewDruidDataSource();
        Connection helperConn = helperDS.getConnection();
        PreparedStatement ps = helperConn.prepareStatement("insert into t_test_string_type(id,char_type,varchar_type,tinyblob_type,tinytext_type,blob_type,text_type,mediumblob_type,mediumtext_type,longblob_type,longtext_type) values(?,?,?,?,?,?,?,?,?,?,?)");
        ps.setInt(1, 1);
        ps.setString(2, "123");
        ps.setString(3, "123");
        InputStream in = getClass().getResourceAsStream("/test_blob.txt");
        ps.setBlob(4, in);
        ps.setString(5, "123");
        ps.setBlob(6, in);
        ps.setString(7, "123");
        ps.setBlob(8, in);
        ps.setString(9, "123");
        ps.setBlob(10, in);
        ps.setString(11, "123");
        ps.execute();
        ps.close();
        helperConn.close();
        in.close();
    }

    private void doUpdateStringTypeData(DataSourceProxy dataSourceProxy) throws Throwable {
        Connection conn = dataSourceProxy.getConnection();
        PreparedStatement ps = conn.prepareStatement("update t_test_string_type set char_type = ?,varchar_type = ?,tinyblob_type = ?,tinytext_type = ?,blob_type = ?,text_type = ? ,mediumblob_type = ?,mediumtext_type = ? ,longblob_type = ?,longtext_type = ? where id=" + testRecordId);
        ps.setString(1, "456");
        ps.setString(2, "456");
        InputStream in = getClass().getResourceAsStream("/test_blob_update.txt");
        ps.setBlob(3, in);
        ps.setString(4, "456");
        ps.setBlob(5, in);
        ps.setString(6, "456");
        ps.setBlob(7, in);
        ps.setString(8, "456");
        ps.setBlob(9, in);
        ps.setString(10, "456");
        ps.executeUpdate();
        ps.close();
        conn.close();
        in.close();
    }


    private void doTestDbTypePhase2(int dataType, boolean globalCommit, String tableName, String updateSql) throws Throwable {
        // init DataSource: helper
        DruidDataSource helperDS = createNewDruidDataSource();

        Connection helperConn = null;
        Statement helperStat = null;
        ResultSet helperRes = null;

        initRM();

        final DataSourceProxy dataSourceProxy = new DataSourceProxy(createNewDruidDataSource());

        RootContext.bind(mockXid);
        Connection testConn = dataSourceProxy.getConnection();
        Statement testStat = testConn.createStatement();

        // >>> query before image
        helperConn = helperDS.getConnection();
        helperStat = helperConn.createStatement();
        helperRes = helperStat.executeQuery("select * from " + tableName + " where id = " + testRecordId);
        TableMeta tableMeta = TableMetaCacheFactory.getTableMetaCache(JdbcConstants.MYSQL).getTableMeta(dataSourceProxy.getPlainConnection(),
                tableName, dataSourceProxy.getResourceId());
        TableRecords beforeImage = TableRecords.buildRecords(tableMeta, helperRes);
        // >>> update record should not throw exception
        if (dataType == STRING_TYPE) {
            Assertions.assertDoesNotThrow(() -> doUpdateStringTypeData(dataSourceProxy));
        } else {
            Assertions.assertDoesNotThrow(() -> testStat.execute(updateSql));
        }
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
            helperRes = helperStat.executeQuery("select * from " + tableName + " where id = " + testRecordId);
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
        druidDataSource.init();
    }
}
