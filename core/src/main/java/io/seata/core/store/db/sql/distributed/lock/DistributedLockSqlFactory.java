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
package io.seata.core.store.db.sql.distributed.lock;

/**
 * @author chd
 */
public class DistributedLockSqlFactory {
    private static final DistributedLockSql DISTRIBUTED_LOCK_SQL = new BaseDistributedLockSql();

    /**
     * get the lock store sql
     *
     * @param dbType the dbType, support mysql/oracle/h2/postgre/oceanbase/dm, it's useless now, but maybe useful later
     * @return lock store sql
     */
    public static DistributedLockSql getDistributedLogStoreSql(String dbType) {
        return DISTRIBUTED_LOCK_SQL;
    }
}
