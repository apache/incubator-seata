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
package io.seata.integration.tx.api.fence.store;

import java.sql.Connection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * The common Fence Store
 *
 * @author kaka2code
 * @author yangwenpeng
 */
public interface CommonFenceStore {

    /**
     * Query common fence do.
     * @param conn the connection
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     * @return the common fence do
     */
    CommonFenceDO queryCommonFenceDO(Connection conn, String xid, Long branchId);

    /**
     * Query xid.
     * @param conn the connection
     * @param datetime the datetime
     * @param limit the limit size
     * @return the tcc fence do
     */
    Set<String> queryEndStatusXidsByDate(Connection conn, Date datetime, int limit);

    /**
     * Insert common fence do boolean.
     * @param conn the connection
     * @param commonFenceDO the common fence do
     * @return the boolean
     */
    boolean insertCommonFenceDO(Connection conn, CommonFenceDO commonFenceDO);

    /**
     * Update common fence do boolean.
     * @param conn the connection
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     * @param newStatus the new status
     * @param oldStatus the old status
     * @return the boolean
     */
    boolean updateCommonFenceDO(Connection conn, String xid, Long branchId, int newStatus, int oldStatus);

    /**
     * Delete common fence do boolean.
     * @param conn the connection
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     * @return the boolean
     */
    boolean deleteCommonFenceDO(Connection conn, String xid, Long branchId);

    /**
     * Delete tcc fence do boolean.
     * @param conn the connection
     * @param xids the global transaction ids
     * @return the boolean
     */
    int deleteTCCFenceDO(Connection conn, List<String> xids);

    /**
     * Set LogTable Name
     * @param logTableName logTableName
     */
    void setLogTableName(String logTableName);

    /**
     * Update application_data
     * @param conn the Connection
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     * @param applicationData the application_data
     * @return the boolean
     */
    boolean updateApplicationData(Connection conn, String xid, Long branchId, String applicationData);

    /**
     * query the application_data
     * @param connection the Connection
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     */
    String queryApplicationData(Connection connection, String xid, long branchId);
}
