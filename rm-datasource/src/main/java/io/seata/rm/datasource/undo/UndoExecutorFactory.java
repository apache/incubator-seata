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
import io.seata.common.exception.ShouldNeverHappenException;

import java.util.HashSet;
import java.util.Set;

/**
 * The type Undo executor factory.
 *
 * @author sharajava
 */
public class UndoExecutorFactory {

    private static final Set<String> UNDO_LOG_SUPPORT_SET = new HashSet<>();

    static {
        UNDO_LOG_SUPPORT_SET.add(JdbcConstants.MYSQL);
        UNDO_LOG_SUPPORT_SET.add(JdbcConstants.ORACLE);
    }

    /**
     * Gets undo executor.
     *
     * @param dbType     the db type
     * @param sqlUndoLog the sql undo log
     * @return the undo executor
     */
    public static AbstractUndoExecutor getUndoExecutor(String dbType, SQLUndoLog sqlUndoLog) {
        if (!UNDO_LOG_SUPPORT_SET.contains(dbType)) {
            throw new NotSupportYetException(dbType);
        }
        AbstractUndoExecutor result = null;
        UndoExecutorHolder holder = UndoExecutorHolderFactory.getUndoExecutorHolder(dbType.toLowerCase());
        switch (sqlUndoLog.getSqlType()) {
            case INSERT:
                result = holder.getInsertExecutor(sqlUndoLog);
                break;
            case UPDATE:
                result = holder.getUpdateExecutor(sqlUndoLog);
                break;
            case DELETE:
                result = holder.getDeleteExecutor(sqlUndoLog);
                break;
            default:
                throw new ShouldNeverHappenException();
        }
        return result;
    }
}
