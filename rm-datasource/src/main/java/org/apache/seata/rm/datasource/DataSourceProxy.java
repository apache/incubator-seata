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
package org.apache.seata.rm.datasource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.Constants;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.Resource;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import org.apache.seata.rm.datasource.undo.UndoLogManager;
import org.apache.seata.rm.datasource.undo.UndoLogManagerFactory;
import org.apache.seata.rm.datasource.util.JdbcUtils;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.seata.common.DefaultValues.DEFAULT_TRANSACTION_UNDO_LOG_TABLE;

/**
 * The type Data source proxy.
 *
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

    private final Map<String, String> variables = new HashMap<>();

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
            checkUndoLogTableExist(connection);

        } catch (SQLException e) {
            throw new IllegalStateException("can not init dataSource", e);
        }
        if (JdbcConstants.SQLSERVER.equals(dbType)) {
            LOGGER.info("SQLServer support in AT mode is currently an experimental function, " +
                    "if you have any problems in use, please feedback to us");
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
     * check existence of undolog table
     *
     * if the table not exist fast fail, or else keep silence
     *
     * @param conn db connection
     */
    private void checkUndoLogTableExist(Connection conn) {
        UndoLogManager undoLogManager;
        try {
            undoLogManager = UndoLogManagerFactory.getUndoLogManager(dbType);
        } catch (EnhancedServiceNotFoundException e) {
            String errMsg = String.format("AT mode don't support the dbtype: %s", dbType);
            throw new IllegalStateException(errMsg, e);
        }

        boolean undoLogTableExist = undoLogManager.hasUndoLogTable(conn);
        if (!undoLogTableExist) {
            String undoLogTableName = ConfigurationFactory.getInstance()
                    .getConfig(ConfigurationKeys.TRANSACTION_UNDO_LOG_TABLE, DEFAULT_TRANSACTION_UNDO_LOG_TABLE);
            String errMsg = String.format("in AT mode, %s table not exist", undoLogTableName);
            throw new IllegalStateException(errMsg);
        }
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
        } else if (JdbcConstants.SQLSERVER.equals(dbType)) {
            initSqlServerResourceId();
        } else if (JdbcConstants.DM.equals(dbType)) {
            initDMResourceId();
        } else if (JdbcConstants.OSCAR.equals(dbType)) {
            initOscarResourceId();
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

    /**
     * init the oscar resource id
     * jdbc:oscar://192.168.x.xx:2003/OSRDB
     */
    private void initOscarResourceId() {
        if (jdbcUrl.contains("?")) {
            resourceId = jdbcUrl.substring(0, jdbcUrl.indexOf('?')) + "/" + userName;
        } else {
            resourceId = jdbcUrl + "/" + userName;
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
        if (resourceId.contains(",")) {
            resourceId = resourceId.replace(",", "|");
        }
    }

    /**
     * The general form of the connection URL for SqlServer is
     * jdbc:sqlserver://[serverName[\instanceName][:portNumber]][;property=value[;property=value]]
     * required connection properties: [INSTANCENAME], [databaseName,database]
     *
     */
    private void initSqlServerResourceId() {
        if (jdbcUrl.contains(";")) {
            StringBuilder jdbcUrlBuilder = new StringBuilder();
            jdbcUrlBuilder.append(jdbcUrl, 0, jdbcUrl.indexOf(';'));
            StringBuilder paramsBuilder = new StringBuilder();
            String paramUrl = jdbcUrl.substring(jdbcUrl.indexOf(';') + 1);
            String[] urlParams = paramUrl.split(";");
            for (String urlParam : urlParams) {
                String[] paramSplit = urlParam.split("=");
                String propertyName = paramSplit[0];
                if ("INSTANCENAME".equalsIgnoreCase(propertyName)
                        || "databaseName".equalsIgnoreCase(propertyName)
                        || "database".equalsIgnoreCase(propertyName)) {
                    paramsBuilder.append(urlParam);
                }
            }

            if (paramsBuilder.length() > 0) {
                jdbcUrlBuilder.append(";");
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

    public String getVariableValue(String name) {
        return variables.get(name);
    }

    private void validMySQLVersion(Connection connection) {
        if (!JdbcConstants.MYSQL.equals(dbType)) {
            return;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement("SHOW VARIABLES");
             ResultSet rs = preparedStatement.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString(1);
                String value = rs.getString(2);
                if (StringUtils.isNotBlank(name)) {
                    variables.put(name.toLowerCase(), value);
                }
            }
            String version = variables.get("version");
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
        } catch (Exception e) {
            LOGGER.error("check mysql version fail error: {}", e.getMessage());
        }
    }
}
