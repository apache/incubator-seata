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
package org.apache.seata.server.coordinator;

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;

/**
 * send outbound request to RM.
 *
 * @since 1.1.0
 */
public interface TransactionCoordinatorOutbound {

    /**
     * Commit a branch transaction.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @return Status of the branch after committing.
     * @throws TransactionException Any exception that fails this will be wrapped with TransactionException and thrown
     *                              out.
     */
    BranchStatus branchCommit(GlobalSession globalSession, BranchSession branchSession) throws TransactionException;

    /**
     * Rollback a branch transaction.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @return Status of the branch after rollbacking.
     * @throws TransactionException Any exception that fails this will be wrapped with TransactionException and thrown
     *                              out.
     */
    BranchStatus branchRollback(GlobalSession globalSession, BranchSession branchSession) throws TransactionException;


}
