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
package org.apache.seata.rm.datasource.undo.sqlserver;

import org.apache.seata.rm.datasource.undo.AbstractUndoExecutor;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;

/**
 * @date 2021-07-26
 */
public abstract class BaseSqlServerUndoExecutor extends AbstractUndoExecutor {
    /**
     * Instantiates a new Abstract undo executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public BaseSqlServerUndoExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    @Override
    protected String buildCheckSql(String tableName, String whereCondition) {
        return "SELECT * FROM " + tableName + " WITH(UPDLOCK) WHERE " + whereCondition;
    }
}
