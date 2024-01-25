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
package org.apache.seata.core.model;

import org.apache.seata.core.exception.TransactionException;

/**
 * Resource Manager: send outbound request to TC.
 *
 */
public interface ResourceManagerOutbound {

    /**
     * Branch register long.
     *
     * @param branchType the branch type
     * @param resourceId the resource id
     * @param clientId   the client id
     * @param xid        the xid
     * @param applicationData the context
     * @param lockKeys   the lock keys
     * @return the long
     * @throws TransactionException the transaction exception
     */
    Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid, String applicationData, String lockKeys) throws
        TransactionException;

    /**
     * Branch report.
     *
     * @param branchType      the branch type
     * @param xid             the xid
     * @param branchId        the branch id
     * @param status          the status
     * @param applicationData the application data
     * @throws TransactionException the transaction exception
     */
    void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status, String applicationData) throws TransactionException;

    /**
     * Lock query boolean.
     *
     * @param branchType the branch type
     * @param resourceId the resource id
     * @param xid        the xid
     * @param lockKeys   the lock keys
     * @return the boolean
     * @throws TransactionException the transaction exception
     */
    boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys)
        throws TransactionException;
}
