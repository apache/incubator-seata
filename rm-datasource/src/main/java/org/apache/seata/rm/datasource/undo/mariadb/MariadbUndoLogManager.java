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
package org.apache.seata.rm.datasource.undo.mariadb;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import org.apache.seata.rm.datasource.undo.UndoLogParser;
import org.apache.seata.rm.datasource.undo.mysql.MySQLUndoLogManager;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.sqlparser.util.JdbcConstants;


@LoadLevel(name = JdbcConstants.MARIADB)
public class MariadbUndoLogManager extends MySQLUndoLogManager {

    @Override
    public int deleteUndoLogByLogCreated(Date logCreated, int limitRows, Connection conn) throws SQLException {
        return super.deleteUndoLogByLogCreated(logCreated, limitRows, conn);
    }

    @Override
    protected void insertUndoLogWithNormal(String xid, long branchId, String rollbackCtx, byte[] undoLogContent,
        Connection conn) throws SQLException {
        super.insertUndoLogWithNormal(xid, branchId, rollbackCtx, undoLogContent, conn);
    }

    @Override
    protected void insertUndoLogWithGlobalFinished(String xid, long branchId, UndoLogParser parser, Connection conn)
        throws SQLException {
        super.insertUndoLogWithGlobalFinished(xid, branchId, parser, conn);
    }
}
