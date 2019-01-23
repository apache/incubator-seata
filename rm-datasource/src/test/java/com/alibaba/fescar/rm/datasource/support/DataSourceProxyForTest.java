package com.alibaba.fescar.rm.datasource.support;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fescar.rm.datasource.ConnectionProxy;
import com.alibaba.fescar.rm.datasource.DataSourceProxy;

import java.sql.Connection;
import java.sql.SQLException;

public class DataSourceProxyForTest extends DataSourceProxy {
    public DataSourceProxyForTest(DruidDataSource targetDataSource) {
        super(targetDataSource);
    }

    public DataSourceProxyForTest(DruidDataSource targetDataSource, String resourceGroupId) {
        super(targetDataSource, resourceGroupId);
    }

    @Override
    public ConnectionProxy getConnection() throws SQLException {
        Connection targetConnection = targetDataSource.getConnection();
        return new ConnectionProxyForTest(this, targetConnection, targetDataSource.getDbType());
    }

    @Override
    public ConnectionProxy getConnection(String username, String password) throws SQLException {
        Connection targetConnection = targetDataSource.getConnection(username, password);
        return new ConnectionProxyForTest(this, targetConnection, targetDataSource.getDbType());
    }


}
