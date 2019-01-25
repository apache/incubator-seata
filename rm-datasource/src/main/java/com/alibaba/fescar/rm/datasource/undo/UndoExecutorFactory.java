/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.rm.datasource.undo;


import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.fescar.common.exception.NotSupportYetException;
import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.rm.datasource.undo.mysql.MySQLUndoDeleteExecutor;
import com.alibaba.fescar.rm.datasource.undo.mysql.MySQLUndoInsertExecutor;
import com.alibaba.fescar.rm.datasource.undo.mysql.MySQLUndoUpdateExecutor;

public class UndoExecutorFactory {

    public static AbstractUndoExecutor getUndoExecutor(String dbType, SQLUndoLog sqlUndoLog) {
        if (!dbType.equals(JdbcConstants.MYSQL)) {
            throw new NotSupportYetException(dbType);
        }
        switch (sqlUndoLog.getSqlType()) {
            case INSERT:
                return new MySQLUndoInsertExecutor(sqlUndoLog);
            case UPDATE:
                return new MySQLUndoUpdateExecutor(sqlUndoLog);
            case DELETE:
                return new MySQLUndoDeleteExecutor(sqlUndoLog);
            default:
                throw new ShouldNeverHappenException();
        }
    }
}
