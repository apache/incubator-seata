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
package org.apache.seata.integration.tx.api.fence.store.db.sql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CommonFenceStoreSqlsTest {

    private static final String TABLE = "tcc_fence_log";

    /**
     * The date-based cleanup selects distinct xids, so a row limit bounds the number of distinct xids
     * (one xid may own multiple branch rows) and the limit comparison in the cleanup loop stays consistent.
     */
    @Test
    public void queryEndStatusByDateSelectsDistinctXids() {
        assertEquals(
                "select distinct xid  from tcc_fence_log where  gmt_modified < ?  and status in (2 , 3 , 4) limit ? ",
                CommonFenceStoreSqls.getQueryEndStatusSQLByDate(TABLE, false));

        assertEquals(
                "select distinct xid  from tcc_fence_log where  gmt_modified < ?  and status in (2 , 3 , 4) and ROWNUM <= ? ",
                CommonFenceStoreSqls.getQueryEndStatusSQLByDate(TABLE, true));
    }

    /**
     * Core regression: deleting expired fence logs by xid is also restricted by gmt_modified and end status.
     * Without these predicates, deleting by xid alone would purge sibling branch rows of the same global
     * transaction that are still in progress (TRIED) or not yet expired.
     */
    @Test
    public void deleteByXidsIsRestrictedByDateAndEndStatus() {
        assertEquals(
                "delete from tcc_fence_log where xid in (?, ?) and gmt_modified < ?  and status in (2 , 3 , 4)",
                CommonFenceStoreSqls.getDeleteSQLByXids(TABLE, "?, ?"));
    }
}
