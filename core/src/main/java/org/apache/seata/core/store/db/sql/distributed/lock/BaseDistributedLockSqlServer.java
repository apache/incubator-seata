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
package org.apache.seata.core.store.db.sql.distributed.lock;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.core.constants.ServerTableColumnsName;

@LoadLevel(name = "sqlserver")
public class BaseDistributedLockSqlServer extends BaseDistributedLockSql {

    protected static final String SELECT_FOR_UPDATE_SQL = "SELECT " + ALL_COLUMNS + " FROM " + DISTRIBUTED_LOCK_TABLE_PLACE_HOLD
            + " WITH (ROWLOCK, UPDLOCK, HOLDLOCK) WHERE " + ServerTableColumnsName.DISTRIBUTED_LOCK_KEY + " = ?";

    @Override
    public String getSelectDistributeForUpdateSql(String distributedLockTable) {
        return SELECT_FOR_UPDATE_SQL.replace(DISTRIBUTED_LOCK_TABLE_PLACE_HOLD, distributedLockTable);
    }

}
