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
package org.apache.seata.server.session;

import java.util.Collection;
import java.util.List;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.rpc.Disposable;

/**
 * The interface Session manager.
 *
 */
public interface SessionManager extends Disposable {

    /**
     * Add global session.
     *
     * @param session the session
     * @throws TransactionException the transaction exception
     */
    void addGlobalSession(GlobalSession session) throws TransactionException;

    /**
     * Find global session global session.
     *
     * @param xid the xid
     * @return the global session
     */
    GlobalSession findGlobalSession(String xid) ;

    /**
     * Find global session global session.
     *
     * @param xid the xid
     * @param withBranchSessions the withBranchSessions
     * @return the global session
     */
    GlobalSession findGlobalSession(String xid, boolean withBranchSessions);

    /**
     * Update global session status.
     *
     * @param session the session
     * @param status  the status
     * @throws TransactionException the transaction exception
     */
    void updateGlobalSessionStatus(GlobalSession session, GlobalStatus status) throws TransactionException;

    /**
     * Remove global session.
     *
     * @param session the session
     * @throws TransactionException the transaction exception
     */
    void removeGlobalSession(GlobalSession session) throws TransactionException;

    /**
     * Add branch session.
     *
     * @param globalSession the global session
     * @param session       the session
     * @throws TransactionException the transaction exception
     */
    void addBranchSession(GlobalSession globalSession, BranchSession session) throws TransactionException;

    /**
     * Update branch session status.
     *
     * @param session the session
     * @param status  the status
     * @throws TransactionException the transaction exception
     */
    void updateBranchSessionStatus(BranchSession session, BranchStatus status) throws TransactionException;

    /**
     * Remove branch session.
     *
     * @param globalSession the global session
     * @param session       the session
     * @throws TransactionException the transaction exception
     */
    void removeBranchSession(GlobalSession globalSession, BranchSession session) throws TransactionException;

    /**
     * All sessions collection.
     *
     * @return the collection
     */
    Collection<GlobalSession> allSessions();

    /**
     * Find global sessions list.
     *
     * @param condition the condition
     * @return the list
     */
    List<GlobalSession> findGlobalSessions(SessionCondition condition);

    /**
     * lock and execute
     *
     * @param globalSession the global session
     * @param lockCallable the lock Callable
     * @return the value
     * @throws TransactionException the transaction exception
     */
    @Deprecated
    <T> T lockAndExecute(GlobalSession globalSession, GlobalSession.LockCallable<T> lockCallable)
            throws TransactionException;

    /**
     * On begin.
     *
     * @param globalSession the global session
     * @throws TransactionException the transaction exception
     */
    @Deprecated
    void onBegin(GlobalSession globalSession) throws TransactionException;

    /**
     * On status change.
     *
     * @param globalSession the global session
     * @param status        the status
     * @throws TransactionException the transaction exception
     */
    @Deprecated
    void onStatusChange(GlobalSession globalSession, GlobalStatus status) throws TransactionException;

    /**
     * On branch status change.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @param status        the status
     * @throws TransactionException the transaction exception
     */
    @Deprecated
    void onBranchStatusChange(GlobalSession globalSession, BranchSession branchSession, BranchStatus status)
            throws TransactionException;

    /**
     * On add branch.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws TransactionException the transaction exception
     */
    @Deprecated
    void onAddBranch(GlobalSession globalSession, BranchSession branchSession) throws TransactionException;

    /**
     * On remove branch.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws TransactionException the transaction exception
     */
    @Deprecated
    void onRemoveBranch(GlobalSession globalSession, BranchSession branchSession) throws TransactionException;

    /**
     * On close.
     *
     * @param globalSession the global session
     * @throws TransactionException the transaction exception
     */
    @Deprecated
    void onClose(GlobalSession globalSession) throws TransactionException;

    /**
     * On end.
     *
     * @param globalSession the global session
     * @throws TransactionException the transaction exception
     */
    @Deprecated
    void onSuccessEnd(GlobalSession globalSession) throws TransactionException;

    /**
     * On fail end.
     *
     * @param globalSession the global session
     * @throws TransactionException the transaction exception
     */
    @Deprecated
    void onFailEnd(GlobalSession globalSession) throws TransactionException;
}
