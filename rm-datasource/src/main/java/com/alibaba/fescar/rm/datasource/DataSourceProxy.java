/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.rm.datasource;

import java.sql.Connection;
import java.sql.SQLException;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fescar.core.model.Resource;

/**
 * The type Data source proxy.
 */
public class DataSourceProxy extends AbstractDataSourceProxy implements Resource {

    private String resourceGroupId = "DEFAULT";

    private boolean managed = false;

    /**
     * Instantiates a new Data source proxy.
     *
     * @param targetDataSource the target data source
     */
    public DataSourceProxy(DruidDataSource targetDataSource) {
        super(targetDataSource);
    }

    /**
     * Instantiates a new Data source proxy.
     *
     * @param targetDataSource the target data source
     * @param resourceGroupId  the resource group id
     */
    public DataSourceProxy(DruidDataSource targetDataSource, String resourceGroupId) {
        super(targetDataSource);
        this.resourceGroupId = resourceGroupId;
    }

    private void assertManaged() {
        if (!managed) {
            DataSourceManager.get().registerResource(this);
            managed = true;
        }
    }

    /**
     * Gets plain connection.
     *
     * @return the plain connection
     * @throws SQLException the sql exception
     */
    public Connection getPlainConnection() throws SQLException {
        return targetDataSource.getConnection();
    }

    /**
     * Gets db type.
     *
     * @return the db type
     */
    public String getDbType() {
        return targetDataSource.getDbType();
    }

    @Override
    public ConnectionProxy getConnection() throws SQLException {
        assertManaged();
        Connection targetConnection = targetDataSource.getConnection();
        return new ConnectionProxy(this, targetConnection, targetDataSource.getDbType());
    }

    @Override
    public ConnectionProxy getConnection(String username, String password) throws SQLException {
        assertManaged();
        Connection targetConnection = targetDataSource.getConnection(username, password);
        return new ConnectionProxy(this, targetConnection, targetDataSource.getDbType());
    }

    @Override
    public String getResourceGroupId() {
        return resourceGroupId;
    }

    @Override
    public String getResourceId() {
        return targetDataSource.getUrl();
    }
}
