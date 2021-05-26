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
package io.seata.rm.tcc.store;

import java.sql.Connection;
import java.util.Date;

/**
 * The TCC Fence Store
 *
 * @author kaka2code
 */
public interface TCCFenceStore {

    /**
     * Query tcc fence do.
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     * @return the tcc fence do
     */
    TCCFenceDO queryTCCFenceDO(Connection conn, String xid, Long branchId);

    /**
     * Insert tcc fence do boolean.
     * @param tccFenceDO the tcc fence do
     * @return the boolean
     */
    boolean insertTCCFenceDO(Connection conn, TCCFenceDO tccFenceDO);

    /**
     * Update tcc fence do boolean.
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     * @param newStatus the new status
     * @return the boolean
     */
    boolean updateTCCFenceDO(Connection conn, String xid, Long branchId, int newStatus, int oldStatus);

    /**
     * Delete tcc fence do boolean.
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     * @return the boolean
     */
    boolean deleteTCCFenceDO(Connection conn, String xid, Long branchId);

    /**
     * Delete tcc fence by datetime.
     * @param datetime datetime
     * @return the deleted row count
     */
    int deleteTCCFenceDOByDate(Connection conn, Date datetime);

    /**
     * Set LogTable Name
     * @param logTableName logTableName
     */
    void setLogTableName(String logTableName);

}
