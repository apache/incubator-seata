package io.seata.supportDataBaseDataType.oracle;

import java.sql.SQLException;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.util.JdbcUtils;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.datasource.DataSourceManager;

/**
 * AT transaction mode tests to support database data types
 *
 * author doubleDimple
 */
public class SupportDataBaseDataTypeForOracleTest {

    private static final String oracle_jdbcUrl = "****";
    private static final String oracle_username = "****";
    private static final String oracle_password = "****";


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
                return super.branchRegister(branchType, resourceId, clientId, xid, applicationData, lockKeys);
            }

            @Override
            public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status, String applicationData) throws TransactionException {
                super.branchReport(branchType, xid, branchId, status, applicationData);
            }
        });
    }

}
