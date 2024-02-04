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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.rm.datasource.undo.mysql.MySQLUndoInsertExecutor;

/**
 * The type Mariadb undo insert executor.
 *
 */
public class MariadbUndoInsertExecutor extends MySQLUndoInsertExecutor {

    /**
     * Instantiates a new My sql undo insert executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public MariadbUndoInsertExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    @Override
    protected String buildUndoSQL() {
        return super.buildUndoSQL();
    }

    @Override
    protected void undoPrepare(PreparedStatement undoPST, ArrayList<Field> undoValues, List<Field> pkValueList)
        throws SQLException {
        super.undoPrepare(undoPST, undoValues, pkValueList);
    }

    @Override
    protected TableRecords getUndoRows() {
        return super.getUndoRows();
    }

}
