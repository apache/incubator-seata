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
package org.apache.seata.rm.datasource.undo.clickhouse;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.core.constants.ClientTableColumnsName;
import org.apache.seata.rm.datasource.undo.mysql.MySQLUndoLogManager;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

@LoadLevel(name = JdbcConstants.CLICKHOUSE)
public class ClickhouseUndoLogManager extends MySQLUndoLogManager {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    // ClickHouse uses ALTER TABLE ... DELETE instead of standard DELETE FROM
    private static final String DELETE_UNDO_LOG_BY_CREATE_SQL = "ALTER TABLE " + UNDO_LOG_TABLE_NAME + " DELETE WHERE "
            + ClientTableColumnsName.UNDO_LOG_LOG_CREATED + " <= ?";

    @Override
    public int deleteUndoLogByLogCreated(Date logCreated, int limitRows, Connection conn) throws SQLException {
        try (PreparedStatement deletePST = conn.prepareStatement(DELETE_UNDO_LOG_BY_CREATE_SQL)) {
            // Clickhouse doesn't natively support LIMIT in ALTER TABLE ... DELETE easily inside prepared statements
            // like MySQL
            // so we omit the LIMIT parameter for log sweeping.
            deletePST.setDate(1, new java.sql.Date(logCreated.getTime()));
            int deleteRows = deletePST.executeUpdate();
            if (logger.isDebugEnabled()) {
                logger.debug("batch delete undo log for clickhouse");
            }
            return deleteRows;
        } catch (Exception e) {
            if (!(e instanceof SQLException)) {
                e = new SQLException(e);
            }
            throw (SQLException) e;
        }
    }
}
