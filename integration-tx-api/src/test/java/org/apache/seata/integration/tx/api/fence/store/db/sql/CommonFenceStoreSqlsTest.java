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

import org.apache.seata.integration.tx.api.fence.constant.CommonFenceConstant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommonFenceStoreSqlsTest {

    private static final String TABLE = "tcc_fence_log";

    private static final String END_STATUS_IN = "status in (" + CommonFenceConstant.STATUS_COMMITTED + " , "
            + CommonFenceConstant.STATUS_ROLLBACKED + " , " + CommonFenceConstant.STATUS_SUSPENDED + ")";

    /**
     * The date-based cleanup must select distinct xids, so a row limit bounds the number of distinct xids
     * (one xid may own multiple branch rows) and the limit comparison in the cleanup loop stays consistent.
     */
    @Test
    public void queryEndStatusByDateSelectsDistinctXids() {
        String mysql = CommonFenceStoreSqls.getQueryEndStatusSQLByDate(TABLE, false);
        assertTrue(mysql.contains("select distinct xid"), mysql);
        assertTrue(mysql.contains("gmt_modified <"), mysql);
        assertTrue(mysql.contains(END_STATUS_IN), mysql);
        assertTrue(mysql.contains("limit ?"), mysql);

        String oracle = CommonFenceStoreSqls.getQueryEndStatusSQLByDate(TABLE, true);
        assertTrue(oracle.contains("ROWNUM <= ?"), oracle);
    }

    /**
     * Core regression: deleting expired fence logs by xid must also be restricted by gmt_modified and end status.
     * Without these predicates, deleting by xid alone would purge sibling branch rows of the same global
     * transaction that are still in progress (TRIED) or not yet expired.
     */
    @Test
    public void deleteByXidsIsRestrictedByDateAndEndStatus() {
        String sql = CommonFenceStoreSqls.getDeleteSQLByXids(TABLE, "?, ?");

        assertTrue(sql.contains("xid in (?, ?)"), sql);
        // must not be a bare delete-by-xid: the date and end-status guards have to be present
        assertTrue(sql.contains("gmt_modified <"), sql);
        assertTrue(sql.contains(END_STATUS_IN), sql);
        // the in-progress status must never be a deletion target
        assertFalse(sql.contains("status in (" + CommonFenceConstant.STATUS_TRIED), sql);
    }
}
