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

/**
 * The Type UndoExecutorHolder
 *
 * @author: Zhibei Hao
 */
public interface UndoExecutorHolder {

    /**
     * get the specific Insert UndoExecutor by sqlUndoLog
     *
     * @param sqlUndoLog the sqlUndoLog
     * @return the specific UndoExecutor
     */
    AbstractUndoExecutor getInsertExecutor(SQLUndoLog sqlUndoLog);

    /**
     * get the specific Update UndoExecutor by sqlUndoLog
     *
     * @param sqlUndoLog the sqlUndoLog
     * @return the specific UndoExecutor
     */
    AbstractUndoExecutor getUpdateExecutor(SQLUndoLog sqlUndoLog);

    /**
     * get the specific Delete UndoExecutor by sqlUndoLog
     *
     * @param sqlUndoLog the sqlUndoLog
     * @return the specific UndoExecutor
     */
    AbstractUndoExecutor getDeleteExecutor(SQLUndoLog sqlUndoLog);

    /**
     * get the SQL type of the current UndoExecutorHolder
     *
     * @return the SQL type string
     */
    String getDbType();
}
