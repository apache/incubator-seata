package io.seata.at;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.util.JdbcUtils;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.datasource.DataSourceManager;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * add AT transaction mode tests to support database data types (Oracle)
 *
 * author doubleDimple
 */
public class ATModeSupportDataBaseDataTypeTest {

    private static final String oracle_jdbcUrl = "jdbc:oracle:thin:@//localhost:49161/XE";
    private static final String oracle_username = "system";
    private static final String oracle_password = "oracle";


    public DruidDataSource getDruidDataSource() throws SQLException {
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setUrl(oracle_jdbcUrl);
        druidDataSource.setUsername(oracle_username);
        druidDataSource.setPassword(oracle_password);
        druidDataSource.setDriverClassName(JdbcUtils.ORACLE_DRIVER);
        druidDataSource.init();

        return druidDataSource;
    }


    public void initResourceManager() {
        DefaultResourceManager.mockResourceManager(BranchType.AT,new DataSourceManager() {
            @Override
            public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid, String applicationData, String lockKeys) throws TransactionException {

                return null;
            }

            @Override
            public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status, String applicationData) throws TransactionException {

            }
        });
    }

    @Test
    public void testConnection() throws SQLException {
        DruidDataSource druidDataSource = getDruidDataSource();
        Statement statement = druidDataSource.getConnection().createStatement();
        System.out.println(druidDataSource);
    }
}
