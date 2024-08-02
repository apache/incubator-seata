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
import java.sql.SQLException;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.rm.datasource.SeataDataSourceProxyMetadata;
import org.apache.seata.rm.datasource.undo.UndoLogManager;
import org.apache.seata.rm.datasource.undo.UndoLogManagerFactory;
import org.apache.seata.rm.datasource.util.JdbcUtils;

import static org.apache.seata.common.DefaultValues.DEFAULT_TRANSACTION_UNDO_LOG_TABLE;

public abstract class AbstractDataSourceProxyMetadata implements SeataDataSourceProxyMetadata {

    protected String jdbcUrl;
    protected String dbType;
    protected String userName;

    @Override
    public void init(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            jdbcUrl = connection.getMetaData().getURL();
            dbType = JdbcUtils.getDbType(jdbcUrl);
            userName = connection.getMetaData().getUserName();
            checkUndoLogTableExist(connection);
        }
    }

    /**
     * check existence of undolog table
     * <p>
     * if the table not exist fast fail, or else keep silence
     *
     * @param conn db connection
     */
    protected void checkUndoLogTableExist(Connection conn) {
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

    @Override
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    @Override
    public String getDbType() {
        return dbType;
    }

    @Override
    public String getUserName() {
        return userName;
    }
}
