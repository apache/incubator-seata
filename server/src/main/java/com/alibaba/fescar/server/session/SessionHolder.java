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
package com.alibaba.fescar.server.session;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.GlobalStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Session holder.
 */
public class SessionHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionHolder.class);

    /**
     * The constant ROOT_SESSION_MANAGER_NAME.
     */
    public static final String ROOT_SESSION_MANAGER_NAME = "root.data";
    /**
     * The constant ASYNC_COMMITTING_SESSION_MANAGER_NAME.
     */
    public static final String ASYNC_COMMITTING_SESSION_MANAGER_NAME = "async.commit.data";
    /**
     * The constant RETRY_COMMITTING_SESSION_MANAGER_NAME.
     */
    public static final String RETRY_COMMITTING_SESSION_MANAGER_NAME = "retry.commit.data";
    /**
     * The constant RETRY_ROLLBACKING_SESSION_MANAGER_NAME.
     */
    public static final String RETRY_ROLLBACKING_SESSION_MANAGER_NAME = "retry.rollback.data";

    private static SessionManager ROOT_SESSION_MANAGER;
    private static SessionManager ASYNC_COMMITTING_SESSION_MANAGER;
    private static SessionManager RETRY_COMMITTING_SESSION_MANAGER;
    private static SessionManager RETRY_ROLLBACKING_SESSION_MANAGER;

    /**
     * Init.
     *
     * @param sessionStorePath the session store path
     * @throws IOException the io exception
     */
    public static void init(String sessionStorePath) throws IOException {
        if (sessionStorePath == null) {
            ROOT_SESSION_MANAGER = new DefaultSessionManager(ROOT_SESSION_MANAGER_NAME);
            ASYNC_COMMITTING_SESSION_MANAGER = new DefaultSessionManager(ASYNC_COMMITTING_SESSION_MANAGER_NAME);
            RETRY_COMMITTING_SESSION_MANAGER = new DefaultSessionManager(RETRY_COMMITTING_SESSION_MANAGER_NAME);
            RETRY_ROLLBACKING_SESSION_MANAGER = new DefaultSessionManager(RETRY_ROLLBACKING_SESSION_MANAGER_NAME);
        } else {
            if (!sessionStorePath.endsWith(File.separator)) {
                sessionStorePath = sessionStorePath + File.separator;
            }
            ROOT_SESSION_MANAGER = new FileBasedSessionManager(ROOT_SESSION_MANAGER_NAME, sessionStorePath);
            ASYNC_COMMITTING_SESSION_MANAGER = new DefaultSessionManager(ASYNC_COMMITTING_SESSION_MANAGER_NAME);
            RETRY_COMMITTING_SESSION_MANAGER = new DefaultSessionManager(RETRY_COMMITTING_SESSION_MANAGER_NAME);
            RETRY_ROLLBACKING_SESSION_MANAGER = new DefaultSessionManager(RETRY_ROLLBACKING_SESSION_MANAGER_NAME);

        }

        if (ROOT_SESSION_MANAGER instanceof Reloadable) {
            ((Reloadable)ROOT_SESSION_MANAGER).reload();

            Collection<GlobalSession> reloadedSessions = ROOT_SESSION_MANAGER.allSessions();
            if (reloadedSessions != null && !reloadedSessions.isEmpty()) {
                reloadedSessions.forEach(globalSession -> {
                    GlobalStatus globalStatus = globalSession.getStatus();
                    switch (globalStatus) {
                        case UnKnown:
                        case Committed:
                        case CommitFailed:
                        case Rollbacked:
                        case RollbackFailed:
                        case TimeoutRollbacked:
                        case TimeoutRollbackFailed:
                        case Finished:
                            throw new ShouldNeverHappenException("Reloaded Session should NOT be " + globalStatus);
                        case AsyncCommitting:
                            try {
                                globalSession.addSessionLifecycleListener(getAsyncCommittingSessionManager());
                                getAsyncCommittingSessionManager().addGlobalSession(globalSession);
                            } catch (TransactionException e) {
                                throw new ShouldNeverHappenException(e);
                            }
                            break;
                        default: {
                            ArrayList<BranchSession> branchSessions = globalSession.getSortedBranches();
                            // Lock
                            branchSessions.forEach(branchSession -> {
                                try {
                                    branchSession.lock();
                                } catch (TransactionException e) {
                                    throw new ShouldNeverHappenException(e);
                                }
                            });

                            switch (globalStatus) {
                                case Committing:
                                case CommitRetrying:
                                    try {
                                        globalSession.addSessionLifecycleListener(
                                            getRetryCommittingSessionManager());
                                        getRetryCommittingSessionManager().addGlobalSession(globalSession);
                                    } catch (TransactionException e) {
                                        throw new ShouldNeverHappenException(e);
                                    }
                                    break;
                                case Rollbacking:
                                case RollbackRetrying:
                                case TimeoutRollbacking:
                                case TimeoutRollbackRetrying:
                                    try {
                                        globalSession.addSessionLifecycleListener(
                                            getRetryRollbackingSessionManager());
                                        getRetryRollbackingSessionManager().addGlobalSession(globalSession);
                                    } catch (TransactionException e) {
                                        throw new ShouldNeverHappenException(e);
                                    }
                                    break;
                                case Begin:
                                    globalSession.setActive(true);
                                    break;
                                default:
                                    throw new ShouldNeverHappenException("NOT properly handled " + globalStatus);
                            }

                            break;

                        }
                    }

                });
            }
        }

    }

    /**
     * Gets root session manager.
     *
     * @return the root session manager
     */
    public static final SessionManager getRootSessionManager() {
        if (ROOT_SESSION_MANAGER == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return ROOT_SESSION_MANAGER;
    }

    /**
     * Gets async committing session manager.
     *
     * @return the async committing session manager
     */
    public static final SessionManager getAsyncCommittingSessionManager() {
        if (ASYNC_COMMITTING_SESSION_MANAGER == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return ASYNC_COMMITTING_SESSION_MANAGER;
    }

    /**
     * Gets retry committing session manager.
     *
     * @return the retry committing session manager
     */
    public static final SessionManager getRetryCommittingSessionManager() {
        if (RETRY_COMMITTING_SESSION_MANAGER == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return RETRY_COMMITTING_SESSION_MANAGER;
    }

    /**
     * Gets retry rollbacking session manager.
     *
     * @return the retry rollbacking session manager
     */
    public static final SessionManager getRetryRollbackingSessionManager() {
        if (RETRY_ROLLBACKING_SESSION_MANAGER == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return RETRY_ROLLBACKING_SESSION_MANAGER;
    }

    /**
     * Find global session global session.
     *
     * @param transactionId the transaction id
     * @return the global session
     * @throws TransactionException the transaction exception
     */
    public static GlobalSession findGlobalSession(Long transactionId) throws TransactionException {
        return getRootSessionManager().findGlobalSession(transactionId);
    }
}
