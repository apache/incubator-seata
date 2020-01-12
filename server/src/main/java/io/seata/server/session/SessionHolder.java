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
package io.seata.server.session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.exception.StoreException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.StoreMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Session holder.
 *
 * @author sharajava
 */
public class SessionHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionHolder.class);

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * The constant DEFAULT.
     */
    public static final String DEFAULT = "default";

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

    /**
     * The default session store dir
     */
    public static final String DEFAULT_SESSION_STORE_FILE_DIR = "sessionStore";

    private static SessionManager ROOT_SESSION_MANAGER;
    private static SessionManager ASYNC_COMMITTING_SESSION_MANAGER;
    private static SessionManager RETRY_COMMITTING_SESSION_MANAGER;
    private static SessionManager RETRY_ROLLBACKING_SESSION_MANAGER;

    /**
     * Init.
     *
     * @param mode the store mode: file, db
     * @throws IOException the io exception
     */
    public static void init(String mode) throws IOException {
        if (StringUtils.isBlank(mode)) {
            //use default
            mode = CONFIG.getConfig(ConfigurationKeys.STORE_MODE);
        }
        //the store mode
        StoreMode storeMode = StoreMode.valueof(mode);
        if (StoreMode.DB.equals(storeMode)) {
            //database store
            ROOT_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, StoreMode.DB.name());
            ASYNC_COMMITTING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, StoreMode.DB.name(),
                new Object[] {ASYNC_COMMITTING_SESSION_MANAGER_NAME});
            RETRY_COMMITTING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, StoreMode.DB.name(),
                new Object[] {RETRY_COMMITTING_SESSION_MANAGER_NAME});
            RETRY_ROLLBACKING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, StoreMode.DB.name(),
                new Object[] {RETRY_ROLLBACKING_SESSION_MANAGER_NAME});
        } else if (StoreMode.FILE.equals(storeMode)) {
            //file store
            String sessionStorePath = CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR,
                DEFAULT_SESSION_STORE_FILE_DIR);
            if (sessionStorePath == null) {
                throw new StoreException("the {store.file.dir} is empty.");
            }
            ROOT_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, StoreMode.FILE.name(),
                new Object[] {ROOT_SESSION_MANAGER_NAME, sessionStorePath});
            ASYNC_COMMITTING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, DEFAULT,
                new Object[] {ASYNC_COMMITTING_SESSION_MANAGER_NAME});
            RETRY_COMMITTING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, DEFAULT,
                new Object[] {RETRY_COMMITTING_SESSION_MANAGER_NAME});
            RETRY_ROLLBACKING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, DEFAULT,
                new Object[] {RETRY_ROLLBACKING_SESSION_MANAGER_NAME});
        } else {
            //unknown store
            throw new IllegalArgumentException("unknown store mode:" + mode);
        }
        //relaod
        reload();
    }

    /**
     * Reload.
     */
    protected static void reload() {
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
                                        globalSession.addSessionLifecycleListener(getRetryCommittingSessionManager());
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
                                        globalSession.addSessionLifecycleListener(getRetryRollbackingSessionManager());
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
     * Find global session.
     *
     * @param xid the xid
     * @return the global session
     */
    public static GlobalSession findGlobalSession(String xid) {
        return findGlobalSession(xid, true);
    }

    /**
     * Find global session.
     *
     * @param xid                the xid
     * @param withBranchSessions the withBranchSessions
     * @return the global session
     */
    public static GlobalSession findGlobalSession(String xid, boolean withBranchSessions) {
        return getRootSessionManager().findGlobalSession(xid, withBranchSessions);
    }

    public static void destroy() {
        ROOT_SESSION_MANAGER.destroy();
        ASYNC_COMMITTING_SESSION_MANAGER.destroy();
        RETRY_COMMITTING_SESSION_MANAGER.destroy();
        RETRY_ROLLBACKING_SESSION_MANAGER.destroy();
    }
}
