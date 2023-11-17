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
package io.seata.rm.datasource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import io.seata.common.Constants;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import io.seata.rm.datasource.util.JdbcUtils;
import io.seata.sqlparser.util.JdbcConstants;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Data source proxy.
 *
 * @author sharajava
 */
public class DataSourceProxy extends AbstractDataSourceProxy implements Resource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceProxy.class);

    private static final String DEFAULT_RESOURCE_GROUP_ID = "DEFAULT";

    private String resourceGroupId;

    private String jdbcUrl;

    private String resourceId;

    private String dbType;

    private String userName;

    private String kernelVersion;

    private String productVersion;

    /**
     * POLARDB-X 1.X -> TDDL
     * POLARDB-X 2.X & MySQL 5.6 -> PXC
     * POLARDB-X 2.X & MySQL 5.7 -> AliSQL-X
     */
    private static final String[] POLARDB_X_PRODUCT_KEYWORD = {"TDDL","AliSQL-X","PXC"};

    /**
     * Instantiates a new Data source proxy.
     *
     * @param targetDataSource the target data source
     */
    public DataSourceProxy(DataSource targetDataSource) {
        this(targetDataSource, DEFAULT_RESOURCE_GROUP_ID);
    }

    /**
     * Instantiates a new Data source proxy.
     *
     * @param targetDataSource the target data source
     * @param resourceGroupId  the resource group id
     */
    public DataSourceProxy(DataSource targetDataSource, String resourceGroupId) {
        if (targetDataSource instanceof SeataDataSourceProxy) {
            LOGGER.info("Unwrap the target data source, because the type is: {}", targetDataSource.getClass().getName());
            targetDataSource = ((SeataDataSourceProxy) targetDataSource).getTargetDataSource();
        }
        this.targetDataSource = targetDataSource;
        init(targetDataSource, resourceGroupId);
    }

    private void init(DataSource dataSource, String resourceGroupId) {
        this.resourceGroupId = resourceGroupId;
        try (Connection connection = dataSource.getConnection()) {
            jdbcUrl = connection.getMetaData().getURL();
            dbType = JdbcUtils.getDbType(jdbcUrl);
            if (JdbcConstants.ORACLE.equals(dbType)) {
                userName = connection.getMetaData().getUserName();
            } else if (JdbcConstants.MYSQL.equals(dbType)) {
                validMySQLVersion(connection);
                checkDerivativeProduct();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("can not init dataSource", e);
        }
        initResourceId();
        DefaultResourceManager.get().registerResource(this);
        TableMetaCacheFactory.registerTableMeta(this);
        //Set the default branch type to 'AT' in the RootContext.
        RootContext.setDefaultBranchType(this.getBranchType());
    }

    /**
     * Define derivative product version for MySQL Kernel
     *
     */
    private void checkDerivativeProduct() {
        if (!JdbcConstants.MYSQL.equals(dbType)) {
            return;
        }
        // check for polardb-x
        if (isPolardbXProduct()) {
            dbType = JdbcConstants.POLARDBX;
            return;
        }
        // check for other products base on mysql kernel
    }

    private boolean isPolardbXProduct() {
        if (StringUtils.isBlank(productVersion)) {
            return false;
        }
        for (String keyword : POLARDB_X_PRODUCT_KEYWORD) {
            if (productVersion.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * publish tableMeta refresh event
     */
    public void tableMetaRefreshEvent() {
        TableMetaCacheFactory.tableMetaRefreshEvent(this.getResourceId());
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
        return dbType;
    }

    @Override
    public ConnectionProxy getConnection() throws SQLException {
        Connection targetConnection = targetDataSource.getConnection();
        return new ConnectionProxy(this, targetConnection);
    }

    @Override
    public ConnectionProxy getConnection(String username, String password) throws SQLException {
        Connection targetConnection = targetDataSource.getConnection(username, password);
        return new ConnectionProxy(this, targetConnection);
    }

    @Override
    public String getResourceGroupId() {
        return resourceGroupId;
    }

    @Override
    public String getResourceId() {
        if (resourceId == null) {
            initResourceId();
        }
        return resourceId;
    }

    private void initResourceId() {
        if (JdbcConstants.POSTGRESQL.equals(dbType)) {
            initPGResourceId();
        } else if (JdbcConstants.ORACLE.equals(dbType) && userName != null) {
            initOracleResourceId();
        } else if (JdbcConstants.MYSQL.equals(dbType) || JdbcConstants.POLARDBX.equals(dbType)) {
            initMysqlResourceId();
        } else if (JdbcConstants.DM.equals(dbType)) {
            initDMResourceId();
        } else {
            initDefaultResourceId();
        }
    }

    /**
     * init the default resource id
     */
    private void initDefaultResourceId() {
        if (jdbcUrl.contains("?")) {
            resourceId = jdbcUrl.substring(0, jdbcUrl.indexOf('?'));
        } else {
            resourceId = jdbcUrl;
        }
    }

    /**
     * init the oracle resource id
     */
    private void initOracleResourceId() {
        if (jdbcUrl.contains("?")) {
            resourceId = jdbcUrl.substring(0, jdbcUrl.indexOf('?')) + "/" + userName;
        } else {
            resourceId = jdbcUrl + "/" + userName;
        }
    }

    /**
     * prevent mysql url like
     * jdbc:mysql:loadbalance://192.168.100.2:3306,192.168.100.1:3306/seata
     * it will cause the problem like
     * 1.rm client is not connected
     */
    private void initMysqlResourceId() {
        String startsWith = "jdbc:mysql:loadbalance://";
        if (jdbcUrl.startsWith(startsWith)) {
            String url;
            if (jdbcUrl.contains("?")) {
                url = jdbcUrl.substring(0, jdbcUrl.indexOf('?'));
            } else {
                url = jdbcUrl;
            }
            resourceId = url.replace(",", "|");
        } else {
            initDefaultResourceId();
        }
    }

    /**
     * prevent pg sql url like
     * jdbc:postgresql://127.0.0.1:5432/seata?currentSchema=public
     * jdbc:postgresql://127.0.0.1:5432/seata?currentSchema=seata
     * cause the duplicated resourceId
     * it will cause the problem like
     * 1.get file lock fail
     * 2.error table meta cache
     */
    private void initPGResourceId() {
        if (jdbcUrl.contains("?")) {
            StringBuilder jdbcUrlBuilder = new StringBuilder();
            jdbcUrlBuilder.append(jdbcUrl, 0, jdbcUrl.indexOf('?'));

            StringBuilder paramsBuilder = new StringBuilder();
            String paramUrl = jdbcUrl.substring(jdbcUrl.indexOf('?') + 1);
            String[] urlParams = paramUrl.split("&");
            for (String urlParam : urlParams) {
                if (urlParam.contains("currentSchema")) {
                    if (urlParam.contains(Constants.DBKEYS_SPLIT_CHAR)) {
                        urlParam = urlParam.replace(Constants.DBKEYS_SPLIT_CHAR, "!");
                    }
                    paramsBuilder.append(urlParam);
                    break;
                }
            }

            if (paramsBuilder.length() > 0) {
                jdbcUrlBuilder.append("?");
                jdbcUrlBuilder.append(paramsBuilder);
            }
            resourceId = jdbcUrlBuilder.toString();
        } else {
            resourceId = jdbcUrl;
        }
    }

    private void initDMResourceId() {
        LOGGER.warn("support for the dameng database is currently an experimental feature ");
        if (jdbcUrl.contains("?")) {
            StringBuilder jdbcUrlBuilder = new StringBuilder();
            jdbcUrlBuilder.append(jdbcUrl, 0, jdbcUrl.indexOf('?'));

            StringBuilder paramsBuilder = new StringBuilder();
            String paramUrl = jdbcUrl.substring(jdbcUrl.indexOf('?') + 1);
            String[] urlParams = paramUrl.split("&");
            for (String urlParam : urlParams) {
                if (urlParam.contains("schema")) {
                    // remove the '"'
                    if (urlParam.contains("\"")) {
                        urlParam = urlParam.replaceAll("\"", "");
                    }
                    paramsBuilder.append(urlParam);
                    break;
                }
            }

            if (paramsBuilder.length() > 0) {
                jdbcUrlBuilder.append("?");
                jdbcUrlBuilder.append(paramsBuilder);
            }
            resourceId = jdbcUrlBuilder.toString();
        } else {
            resourceId = jdbcUrl;
        }
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.AT;
    }

    public String getKernelVersion() {
        return kernelVersion;
    }

    private void validMySQLVersion(Connection connection) {
        if (!JdbcConstants.MYSQL.equals(dbType)) {
            return;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT VERSION()");
             ResultSet versionResult = preparedStatement.executeQuery()) {
            if (versionResult.next()) {
                String version = versionResult.getString("VERSION()");
                if (StringUtils.isBlank(version)) {
                    return;
                }
                int dashIdx = version.indexOf('-');
                // in mysql: 5.6.45, in polardb-x: 5.6.45-TDDL-xxx
                if (dashIdx > 0) {
                    kernelVersion = version.substring(0, dashIdx);
                    productVersion = version.substring(dashIdx + 1);
                } else {
                    kernelVersion = version;
                    productVersion = version;
                }
            }
        } catch (Exception e) {
            LOGGER.error("check mysql version fail error: {}", e.getMessage());
        }
    }
}
