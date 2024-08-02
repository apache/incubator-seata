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
package org.apache.seata.rm.datasource.metadata;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.rm.datasource.util.JdbcUtils;
import org.apache.seata.sqlparser.util.JdbcConstants;

/**
 * mysql datasource proxy metadata
 */
public class MySQLDataSourceProxyMetadata extends AbstractDataSourceProxyMetadata {

    /**
     * POLARDB-X 1.X -> TDDL
     * POLARDB-X 2.X & MySQL 5.6 -> PXC
     * POLARDB-X 2.X & MySQL 5.7 -> AliSQL-X
     */
    private static final String[] POLARDB_X_PRODUCT_KEYWORD = {"TDDL", "AliSQL-X", "PXC"};

    private final Map<String, String> variables = new HashMap<>();
    private String kernelVersion;
    private String productVersion;

    @Override
    public void init(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SHOW VARIABLES");
             ResultSet rs = preparedStatement.executeQuery()) {
            jdbcUrl = connection.getMetaData().getURL();
            dbType = JdbcUtils.getDbType(jdbcUrl);
            userName = connection.getMetaData().getUserName();
            while (rs.next()) {
                String name = rs.getString(1);
                String value = rs.getString(2);
                if (StringUtils.isNotBlank(name)) {
                    variables.put(name.toLowerCase(), value);
                }
            }
            checkUndoLogTableExist(connection);
        }

        validMySQLVersion();
        checkDerivativeProduct();
    }

    @Override
    public String getVariableValue(String name) {
        return variables.get(name);
    }

    @Override
    public String getKernelVersion() {
        return kernelVersion;
    }

    private void validMySQLVersion() {
        String version = variables.get("version");
        if (org.apache.commons.lang.StringUtils.isBlank(version)) {
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

    /**
     * Define derivative product version for MySQL Kernel
     */
    private void checkDerivativeProduct() {
        // check for polardb-x
        if (isPolardbXProduct()) {
            dbType = JdbcConstants.POLARDBX;
        }
        // check for other products base on mysql kernel
    }

    private boolean isPolardbXProduct() {
        if (org.apache.commons.lang.StringUtils.isBlank(productVersion)) {
            return false;
        }
        for (String keyword : POLARDB_X_PRODUCT_KEYWORD) {
            if (productVersion.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
