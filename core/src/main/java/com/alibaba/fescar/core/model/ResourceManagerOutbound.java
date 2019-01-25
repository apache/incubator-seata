/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.core.model;

import com.alibaba.fescar.core.exception.TransactionException;

/**
 * Resource Manager: send outbound request to TC.
 */
public interface ResourceManagerOutbound {

    Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid, String lockKeys) throws
        TransactionException;

    void branchReport(String xid, long branchId, BranchStatus status, String applicationData) throws TransactionException;

    boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys) throws TransactionException;
}
