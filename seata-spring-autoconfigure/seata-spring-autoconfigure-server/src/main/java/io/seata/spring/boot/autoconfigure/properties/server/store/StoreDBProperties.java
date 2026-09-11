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
package io.seata.spring.boot.autoconfigure.properties.server.store;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.common.DefaultValues.DEFAULT_DB_MAX_CONN;
import static io.seata.common.DefaultValues.DEFAULT_DB_MIN_CONN;
import static io.seata.common.DefaultValues.DEFAULT_QUERY_LIMIT;
import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_DB_PREFIX;

/**
 * @author spilledyear@outlook.com
 */
@Component
@ConfigurationProperties(prefix = STORE_DB_PREFIX)
public class StoreDBProperties {
    private String datasource = "druid";
    private String dbType = "mysql";
    private String driverClassName = "com.mysql.jdbc.Driver";
    private String url = "jdbc:mysql://127.0.0.1:3306/seata?rewriteBatchedStatements=true";
    private String user = "mysql";
    private String password = "mysql";
    private Integer minConn = DEFAULT_DB_MIN_CONN;
    private Integer maxConn = DEFAULT_DB_MAX_CONN;
    private String globalTable = "global_table";
    private String branchTable = "branch_table";
    private String lockTable = "lock_table";
    private String distributedLockTable = "distributed_lock";
    private Integer queryLimit = DEFAULT_QUERY_LIMIT;
    private Long maxWait = 5000L;

    public String getDatasource() {
        return datasource;
    }

    public StoreDBProperties setDatasource(String datasource) {
        this.datasource = datasource;
        return this;
    }

    public String getDbType() {
        return dbType;
    }

    public StoreDBProperties setDbType(String dbType) {
        this.dbType = dbType;
        return this;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public StoreDBProperties setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public StoreDBProperties setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getUser() {
        return user;
    }

    public StoreDBProperties setUser(String user) {
        this.user = user;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public StoreDBProperties setPassword(String password) {
        this.password = password;
        return this;
    }

    public Integer getMinConn() {
        return minConn;
    }

    public StoreDBProperties setMinConn(Integer minConn) {
        this.minConn = minConn;
        return this;
    }

    public Integer getMaxConn() {
        return maxConn;
    }

    public StoreDBProperties setMaxConn(Integer maxConn) {
        this.maxConn = maxConn;
        return this;
    }

    public String getGlobalTable() {
        return globalTable;
    }

    public StoreDBProperties setGlobalTable(String globalTable) {
        this.globalTable = globalTable;
        return this;
    }

    public String getBranchTable() {
        return branchTable;
    }

    public StoreDBProperties setBranchTable(String branchTable) {
        this.branchTable = branchTable;
        return this;
    }

    public String getLockTable() {
        return lockTable;
    }

    public StoreDBProperties setLockTable(String lockTable) {
        this.lockTable = lockTable;
        return this;
    }

    public String getDistributedLockTable() {
        return distributedLockTable;
    }

    public void setDistributedLockTable(String distributedLockTable) {
        this.distributedLockTable = distributedLockTable;
    }

    public Integer getQueryLimit() {
        return queryLimit;
    }

    public StoreDBProperties setQueryLimit(Integer queryLimit) {
        this.queryLimit = queryLimit;
        return this;
    }

    public Long getMaxWait() {
        return maxWait;
    }

    public StoreDBProperties setMaxWait(Long maxWait) {
        this.maxWait = maxWait;
        return this;
    }
}
