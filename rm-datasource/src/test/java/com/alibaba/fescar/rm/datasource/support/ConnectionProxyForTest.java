package com.alibaba.fescar.rm.datasource.support;

import com.alibaba.fescar.rm.datasource.ConnectionProxy;
import com.alibaba.fescar.rm.datasource.DataSourceProxy;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionProxyForTest extends ConnectionProxy {

    public ConnectionProxyForTest(DataSourceProxy dataSourceProxy, Connection targetConnection, String dbType) {
        super(dataSourceProxy, targetConnection, dbType);
    }

    @Override
    public void checkLock(String lockKeys) throws SQLException {

    }

    @Override
    public void register(String lockKeys) throws SQLException {

    }
}
