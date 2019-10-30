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
package io.seata.rm.datasource.undo;

import com.alibaba.druid.util.JdbcConstants;
import io.seata.common.exception.NotSupportYetException;
import io.seata.rm.datasource.undo.mysql.MySQLUndoLogManager;
import io.seata.rm.datasource.undo.oracle.OracleUndoLogManager;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jsbxyyx
 * @date 2019/09/07
 */
public final class UndoLogManagerFactory {

    private static final Map<String, UndoLogManager> UNDO_LOG_MANAGER_MAP = new HashMap<>();

    static {
        UNDO_LOG_MANAGER_MAP.put(JdbcConstants.MYSQL, new MySQLUndoLogManager());
        UNDO_LOG_MANAGER_MAP.put(JdbcConstants.ORACLE, new OracleUndoLogManager());
    }

    private UndoLogManagerFactory() {}

    public static UndoLogManager getUndoLogManager(String dbType) {
        UndoLogManager undoLogManager = UNDO_LOG_MANAGER_MAP.get(dbType);
        if (undoLogManager == null) {
            throw new NotSupportYetException("not support dbType[" + dbType + "]");
        }
        return undoLogManager;
    }

}
