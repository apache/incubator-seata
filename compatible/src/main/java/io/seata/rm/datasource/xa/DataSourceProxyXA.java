/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.seata.rm.datasource.xa;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.seata.core.model.BranchType;
import org.apache.seata.rm.datasource.SeataDataSourceProxy;

/**
 * DataSource proxy for XA mode.
 *
 */
@Deprecated
public class DataSourceProxyXA implements SeataDataSourceProxy {

    private final org.apache.seata.rm.datasource.xa.DataSourceProxyXA dataSourceProxyXA;

    public DataSourceProxyXA(DataSource dataSource) {
        this.dataSourceProxyXA = new org.apache.seata.rm.datasource.xa.DataSourceProxyXA(dataSource);
    }

    public DataSourceProxyXA(DataSource dataSource, String resourceGroupId) {
        this.dataSourceProxyXA = new org.apache.seata.rm.datasource.xa.DataSourceProxyXA(dataSource, resourceGroupId);
    }

    @Override
    public DataSource getTargetDataSource() {
        return dataSourceProxyXA.getTargetDataSource();
    }

    @Override
    public BranchType getBranchType() {
        return dataSourceProxyXA.getBranchType();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSourceProxyXA.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return dataSourceProxyXA.getConnection(username, password);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return dataSourceProxyXA.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return dataSourceProxyXA.isWrapperFor(iface);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return dataSourceProxyXA.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        dataSourceProxyXA.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        dataSourceProxyXA.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return dataSourceProxyXA.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return dataSourceProxyXA.getParentLogger();
    }
}
