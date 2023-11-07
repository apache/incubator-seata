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
package io.seata.rm.datasource.undo.mariadb;

import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.rm.datasource.undo.mysql.MySQLUndoDeleteExecutor;

/**
 * The type Mariadb undo delete executor.
 *
 * @author funkye
 */
public class MariadbUndoDeleteExecutor extends MySQLUndoDeleteExecutor {

    /**
     * Instantiates a new Maria db undo delete executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public MariadbUndoDeleteExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    @Override
    protected String buildUndoSQL() {
        return super.buildUndoSQL();
    }

    @Override
    protected TableRecords getUndoRows() {
        return super.getUndoRows();
    }

}
