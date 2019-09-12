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
package io.seata.rm.datasource.undo.postgresql;

import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.undo.UndoTableManager;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author japsercloud
 */
public class PostgresqlUndoTableManager implements UndoTableManager {

    @Override
    public void createTable(DataSourceProxy dataSourceProxy) throws SQLException {
        try (ConnectionProxy connection = dataSourceProxy.getConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE SEQUENCE IF NOT EXISTS \"undo_log_seq\"");
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS \"undo_log\" (\n" +
                    "  \"id\" int8 NOT NULL DEFAULT NULL,\n" +
                    "  \"branch_id\" int8 NOT NULL DEFAULT NULL,\n" +
                    "  \"xid\" varchar(100) NOT NULL DEFAULT NULL,\n" +
                    "  \"context\" varchar(128) NOT NULL DEFAULT NULL,\n" +
                    "  \"rollback_info\" bytea NOT NULL DEFAULT NULL,\n" +
                    "  \"log_status\" int4 NOT NULL DEFAULT NULL,\n" +
                    "  \"log_created\" timestamp(6) NOT NULL DEFAULT NULL,\n" +
                    "  \"log_modified\" timestamp(6) NOT NULL DEFAULT NULL,\n" +
                    "  \"ext\" varchar(100) DEFAULT NULL,\n" +
                    "  CONSTRAINT \"undo_log_pkey\" PRIMARY KEY (\"id\"),\n" +
                    "  CONSTRAINT \"undo_log_branch_id_xid_key\" UNIQUE (\"branch_id\", \"xid\")\n" +
                    ")");
            }
            connection.commit();
        }
    }
}
