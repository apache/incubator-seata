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
package io.seata.rm;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.rm.datasource.SeataDataSourceProxy;
import io.seata.rm.datasource.xa.Holdable;
import io.seata.rm.datasource.xa.Holder;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Base class of those DataSources working as Seata Resource.
 *
 * @author sharajava
 */
public abstract class BaseDataSourceResource<T extends Holdable> implements SeataDataSourceProxy, Resource, Holder<T> {

    protected DataSource dataSource;

    protected String resourceId;

    protected String resourceGroupId;

    protected BranchType branchType;

    protected String dbType;

    protected Driver driver;

    private ConcurrentHashMap<String, T> keeper = new ConcurrentHashMap<>();

    /**
     * Gets target data source.
     *
     * @return the target data source
     */
    @Override
    public DataSource getTargetDataSource() {
        return dataSource;
    }

    @Override
    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public String getResourceGroupId() {
        return resourceGroupId;
    }

    public void setResourceGroupId(String resourceGroupId) {
        this.resourceGroupId = resourceGroupId;
    }

    @Override
    public BranchType getBranchType() {
        return branchType;
    }

    public void setBranchType(BranchType branchType) {
        this.branchType = branchType;
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }


    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface == null) {
            return null;
        }

        if (iface.isInstance(this)) {
            return (T) this;
        }

        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface != null && iface.isInstance(this);

    }

    protected void dataSourceCheck() {
        if (dataSource == null) {
            throw new UnsupportedOperationException("dataSource CAN NOT be null");
        }
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        dataSourceCheck();
        return dataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        dataSourceCheck();
        dataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        dataSourceCheck();
        dataSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        dataSourceCheck();
        return dataSource.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        dataSourceCheck();
        return dataSource.getParentLogger();
    }

    @Override
    public T hold(String key, T value) {
        if (value.isHeld()) {
            T x = keeper.get(key);
            if (x != value) {
                throw new ShouldNeverHappenException("something wrong with keeper, keeping[" + x +
                    "] but[" + value + "] is also kept with the same key[" + key + "]");
            }
            return value;
        }
        T x = keeper.put(key, value);
        value.setHeld(true);
        return x;
    }

    @Override
    public T release(String key, T value) {
        T x = keeper.remove(key);
        if (x != value) {
            throw new ShouldNeverHappenException("something wrong with keeper, released[" + x +
                "] but[" + value + "] is wanted with key[" + key + "]");
        }
        value.setHeld(false);
        return x;
    }

    @Override
    public T lookup(String key) {
        return keeper.get(key);
    }
}
