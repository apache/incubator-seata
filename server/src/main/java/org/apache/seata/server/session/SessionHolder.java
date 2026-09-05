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

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.XID;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.store.LockMode;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.core.store.DistributedLockDO;
import org.apache.seata.core.store.DistributedLocker;
import org.apache.seata.server.cluster.raft.RaftServerManager;
import org.apache.seata.server.cluster.raft.context.SeataClusterContext;
import org.apache.seata.server.lock.LockManager;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.apache.seata.server.lock.distributed.DistributedLockerFactory;
import org.apache.seata.server.storage.file.FileStoreProviderFactory;
import org.apache.seata.server.storage.file.spi.FileLockStore;
import org.apache.seata.server.storage.file.spi.FileStoreContext;
import org.apache.seata.server.storage.file.spi.FileStoreProvider;
import org.apache.seata.server.storage.file.spi.FileStoreRuntime;
import org.apache.seata.server.store.StoreConfig;
import org.apache.seata.server.store.VGroupMappingStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static java.io.File.separator;
import static org.apache.seata.common.DefaultValues.DEFAULT_DISTRIBUTED_LOCK_EXPIRE_TIME;
import static org.apache.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;
import static org.apache.seata.common.DefaultValues.DEFAULT_SESSION_STORE_FILE_DIR;

/**
 * The type Session holder.
 */
public class SessionHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionHolder.class);

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * The constant ROOT_SESSION_MANAGER_NAME.
     */
    public static final String ROOT_SESSION_MANAGER_NAME = "root.data";

    /**
     * The redis distributed lock expire time
     */
    private static long DISTRIBUTED_LOCK_EXPIRE_TIME =
            CONFIG.getLong(ConfigurationKeys.DISTRIBUTED_LOCK_EXPIRE_TIME, DEFAULT_DISTRIBUTED_LOCK_EXPIRE_TIME);

    /**
     * The default vgroup mapping store dir
     */
    public static final String DEFAULT_VGROUP_MAPPING_STORE_FILE_DIR = "vgroupStore";

    private static VGroupMappingStoreManager ROOT_VGROUP_MAPPING_MANAGER;

    private static SessionManager ROOT_SESSION_MANAGER;
    private static volatile Map<String, SessionManager> SESSION_MANAGER_MAP;

    private static DistributedLocker DISTRIBUTED_LOCKER;

    private static volatile FileStoreRuntime FILE_STORE_RUNTIME;

    public static void init() {
        init(null);
    }

    /**
     * Init.
     *
     * @param sessionMode the store mode: file, db, redis
     * @throws IOException the io exception
     */
    public static synchronized void init(SessionMode sessionMode) {
        if (FILE_STORE_RUNTIME != null) {
            throw new StoreException("FileStoreRuntime is already initialized; destroy it before reinitializing");
        }
        if (null == sessionMode) {
            sessionMode = StoreConfig.getSessionMode();
        }
        LOGGER.info("use session store mode: {}", sessionMode.getName());
        LockMode lockMode = StoreConfig.getLockMode();
        boolean fileSessionMode = SessionMode.FILE.equals(sessionMode);
        boolean fileLockMode = LockMode.FILE.equals(lockMode);
        if (fileSessionMode != fileLockMode) {
            String message = fileSessionMode
                    ? "file session mode must use file lock mode"
                    : "file lock mode must use file session mode";
            throw new StoreException(message);
        }
        if (!fileSessionMode) {
            initLegacyMode(sessionMode);
            return;
        }

        FileStoreRuntime runtime = null;
        boolean lockManagerInstalled = false;
        try {
            runtime = openFileStoreRuntime();
            FILE_STORE_RUNTIME = runtime;
            lockManagerInstalled = LockerManagerFactory.init(
                    LockMode.FILE, new Class<?>[] {FileLockStore.class}, new Object[] {runtime.lockStore()});
            initFileMode(runtime);
        } catch (RuntimeException | Error startupFailure) {
            rollbackFailedFileInitialization(startupFailure, runtime, lockManagerInstalled);
            throw startupFailure;
        }
    }

    private static void initLegacyMode(SessionMode sessionMode) {
        DISTRIBUTED_LOCKER = DistributedLockerFactory.getDistributedLocker(sessionMode.getName());
        if (SessionMode.DB.equals(sessionMode)) {
            ROOT_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, SessionMode.DB.getName());
            reload(sessionMode);

            ROOT_VGROUP_MAPPING_MANAGER =
                    EnhancedServiceLoader.load(VGroupMappingStoreManager.class, SessionMode.DB.getName());
        } else if (SessionMode.RAFT.equals(sessionMode)) {
            String group = CONFIG.getConfig(ConfigurationKeys.SERVER_RAFT_GROUP, DEFAULT_SEATA_GROUP);
            ROOT_SESSION_MANAGER = EnhancedServiceLoader.load(
                    SessionManager.class, SessionMode.RAFT.getName(), new Object[] {ROOT_SESSION_MANAGER_NAME});
            SESSION_MANAGER_MAP = new HashMap<>();
            SESSION_MANAGER_MAP.put(group, ROOT_SESSION_MANAGER);
            ROOT_VGROUP_MAPPING_MANAGER =
                    EnhancedServiceLoader.load(VGroupMappingStoreManager.class, SessionMode.RAFT.getName());
            RaftServerManager.init();
            RaftServerManager.start();
        } else if (SessionMode.REDIS.equals(sessionMode)) {
            ROOT_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, SessionMode.REDIS.getName());
            ROOT_VGROUP_MAPPING_MANAGER =
                    EnhancedServiceLoader.load(VGroupMappingStoreManager.class, SessionMode.REDIS.getName());
            reload(sessionMode);
        } else {
            // unknown store
            throw new IllegalArgumentException("unknown store mode:" + sessionMode.getName());
        }
    }

    private static FileStoreRuntime openFileStoreRuntime() {
        String engineName = StoreConfig.getFileEngineName();
        FileStoreProvider provider = FileStoreProviderFactory.getProvider(engineName);
        String sessionStoreDir = CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR, DEFAULT_SESSION_STORE_FILE_DIR);
        if (StringUtils.isBlank(sessionStoreDir)) {
            throw new StoreException("the {store.file.dir} is empty.");
        }
        String sessionStorePath = sessionStoreDir + separator + XID.getPort();
        return provider.open(new FileStoreContext(ROOT_SESSION_MANAGER_NAME, Paths.get(sessionStorePath)));
    }

    private static void initFileMode(FileStoreRuntime runtime) {
        String vGroupMappingStoreDir =
                CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR, DEFAULT_VGROUP_MAPPING_STORE_FILE_DIR);
        if (StringUtils.isBlank(vGroupMappingStoreDir)) {
            throw new StoreException("the {store.file.dir} is empty.");
        }
        String vGroupMappingStorePath = vGroupMappingStoreDir + separator + XID.getPort();
        VGroupMappingStoreManager vGroupMappingManager = EnhancedServiceLoader.load(
                VGroupMappingStoreManager.class, SessionMode.FILE.getName(), new Object[] {vGroupMappingStorePath});
        DistributedLocker distributedLocker = DistributedLockerFactory.getDistributedLocker(SessionMode.FILE.getName());
        ROOT_VGROUP_MAPPING_MANAGER = vGroupMappingManager;
        DISTRIBUTED_LOCKER = distributedLocker;
        ROOT_SESSION_MANAGER = runtime.sessionManager();
        runtime.recover(sessions -> reload(sessions, SessionMode.FILE, true, true));
        runtime.startBackgroundServices();
    }

    private static void rollbackFailedFileInitialization(
            Throwable startupFailure, FileStoreRuntime runtime, boolean lockManagerInstalled) {
        try {
            if (lockManagerInstalled) {
                cleanup(startupFailure, LockerManagerFactory::destroy);
                cleanup(startupFailure, () -> EnhancedServiceLoader.unload(LockManager.class));
            }
            cleanup(startupFailure, runtime == null ? null : runtime::close);
        } finally {
            clearFileModeReferences();
        }
    }

    private static void cleanup(Throwable failure, Runnable action) {
        if (action == null) {
            return;
        }
        try {
            action.run();
        } catch (RuntimeException | Error cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }

    private static void clearFileModeReferences() {
        FILE_STORE_RUNTIME = null;
        ROOT_SESSION_MANAGER = null;
        ROOT_VGROUP_MAPPING_MANAGER = null;
        DISTRIBUTED_LOCKER = null;
        SESSION_MANAGER_MAP = null;
    }

    /**
     * Reload.
     *
     * @param sessionMode the mode of store
     */
    protected static void reload(SessionMode sessionMode) {
        if (sessionMode == SessionMode.FILE && ROOT_SESSION_MANAGER instanceof Reloadable) {
            ((Reloadable) ROOT_SESSION_MANAGER).reload();
            reload(ROOT_SESSION_MANAGER.allSessions(), sessionMode);
        } else if (sessionMode == SessionMode.FILE) {
            reload(ROOT_SESSION_MANAGER.allSessions(), sessionMode);
        } else {
            reload(null, sessionMode);
        }
    }

    public static void reload(Collection<GlobalSession> allSessions, SessionMode storeMode) {
        reload(allSessions, storeMode, true);
    }

    public static void reload(Collection<GlobalSession> allSessions, SessionMode storeMode, boolean acquireLock) {
        reload(allSessions, storeMode, acquireLock, false);
    }

    private static void reload(
            Collection<GlobalSession> allSessions, SessionMode storeMode, boolean acquireLock, boolean failFast) {
        if ((SessionMode.FILE == storeMode || SessionMode.RAFT == storeMode)
                && CollectionUtils.isNotEmpty(allSessions)) {
            long currentTimeMillis = System.currentTimeMillis();
            for (GlobalSession globalSession : allSessions) {
                GlobalStatus globalStatus = globalSession.getStatus();
                switch (globalStatus) {
                    case TimeoutRollbacked:
                    case Rollbacked:
                        try {
                            SessionHelper.endRollbacked(globalSession, true);
                        } catch (TransactionException e) {
                            LOGGER.error(
                                    "Could not handle the global session, xid: {},error: {}",
                                    globalSession.getXid(),
                                    e.getMessage());
                            throwIfFileModeStartupReloadFailed(globalSession, e, failFast);
                        }
                        break;
                    case Committed:
                        try {
                            SessionHelper.endCommitted(globalSession, true);
                        } catch (TransactionException e) {
                            LOGGER.error(
                                    "Could not handle the global session, xid: {},error: {}",
                                    globalSession.getXid(),
                                    e.getMessage());
                            throwIfFileModeStartupReloadFailed(globalSession, e, failFast);
                        }
                        break;
                    case Finished:
                    case UnKnown:
                    case CommitFailed:
                    case RollbackFailed:
                    case TimeoutRollbackFailed:
                        removeInErrorState(globalSession, failFast);
                        break;
                    case AsyncCommitting:
                    case Committing:
                    case CommitRetrying:
                        if (Objects.equals(SessionMode.RAFT, storeMode)) {
                            // When a state change occurs, re-electing the leader may result in the lock not being
                            // unlocked yet
                            // so a COMMIT unlock operation needs to be performed at the time of re-election
                            try {
                                globalSession.clean();
                            } catch (TransactionException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        break;
                    case StopCommitOrCommitRetry:
                    case StopRollbackOrRollbackRetry:
                    case Deleting:
                        break;
                    default: {
                        if (acquireLock) {
                            lockBranchSessions(globalSession.getSortedBranches());
                            if (GlobalStatus.Rollbacking.equals(globalSession.getStatus())
                                    || GlobalStatus.TimeoutRollbacking.equals(globalSession.getStatus())) {
                                globalSession.getBranchSessions().parallelStream()
                                        .forEach(branchSession -> branchSession.setLockStatus(LockStatus.Rollbacking));
                            }
                        }
                        switch (globalStatus) {
                            case Rollbacking:
                            case RollbackRetrying:
                            case TimeoutRollbacking:
                            case TimeoutRollbackRetrying:
                                break;
                            case Begin:
                                if (Objects.equals(storeMode, SessionMode.RAFT)) {
                                    // Avoid rolling back the global session created after becoming the leader.
                                    if (globalSession.getBeginTime() < currentTimeMillis) {
                                        try {
                                            globalSession.changeGlobalStatus(GlobalStatus.RollbackRetrying);
                                            LOGGER.info(
                                                    "change global status: {}, xid: {}",
                                                    globalSession.getStatus(),
                                                    globalSession.getXid());
                                        } catch (TransactionException e) {
                                            LOGGER.error("change global status fail: {}", e.getMessage(), e);
                                        }
                                    } else {
                                        globalSession.setActive(true);
                                    }
                                } else {
                                    globalSession.setActive(true);
                                }
                                break;
                            default:
                                LOGGER.error("Could not handle the global session, xid: {}", globalSession.getXid());
                                throw new ShouldNeverHappenException("NOT properly handled " + globalStatus);
                        }
                        break;
                    }
                }
            }
        } else {
            // Redis, db and so on
            CompletableFuture.runAsync(() -> {
                SessionCondition searchCondition = new SessionCondition(
                        GlobalStatus.UnKnown,
                        GlobalStatus.Committed,
                        GlobalStatus.Rollbacked,
                        GlobalStatus.TimeoutRollbacked,
                        GlobalStatus.Finished);
                searchCondition.setLazyLoadBranch(true);

                long now = System.currentTimeMillis();
                List<GlobalSession> errorStatusGlobalSessions =
                        ROOT_SESSION_MANAGER.findGlobalSessions(searchCondition);
                while (!CollectionUtils.isEmpty(errorStatusGlobalSessions)) {
                    for (GlobalSession errorStatusGlobalSession : errorStatusGlobalSessions) {
                        if (errorStatusGlobalSession.getBeginTime() >= now) {
                            // Exit when the global transaction begin after the instance started
                            return;
                        }

                        removeInErrorState(errorStatusGlobalSession);
                    }

                    // Load the next part
                    errorStatusGlobalSessions = ROOT_SESSION_MANAGER.findGlobalSessions(searchCondition);
                }
            });
        }
    }

    private static void throwIfFileModeStartupReloadFailed(
            GlobalSession globalSession, TransactionException cause, boolean failFast) {
        if (failFast) {
            throw new StoreException(cause, "FileMode startup reload failed, xid: " + globalSession.getXid());
        }
    }

    private static void removeInErrorState(GlobalSession globalSession) {
        removeInErrorState(globalSession, false);
    }

    private static void removeInErrorState(GlobalSession globalSession, boolean failFast) {
        try {
            LOGGER.warn(
                    "The global session should NOT be {}, remove it. xid = {}",
                    globalSession.getStatus(),
                    globalSession.getXid());
            getRootSessionManager().removeGlobalSession(globalSession);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        "Remove global session succeed, xid = {}, status = {}",
                        globalSession.getXid(),
                        globalSession.getStatus());
            }
        } catch (Exception e) {
            LOGGER.error(
                    "Remove global session failed, xid = {}, status = {}",
                    globalSession.getXid(),
                    globalSession.getStatus(),
                    e);
            if (failFast) {
                throw new StoreException(e, "FileMode startup reload failed, xid: " + globalSession.getXid());
            }
        }
    }

    private static void lockBranchSessions(List<BranchSession> branchSessions) {
        branchSessions.forEach(branchSession -> {
            try {
                branchSession.lock();
            } catch (TransactionException e) {
                throw new ShouldNeverHappenException(e);
            }
        });
    }

    // region get group mapping manager
    public static VGroupMappingStoreManager getRootVGroupMappingManager() {
        if (ROOT_VGROUP_MAPPING_MANAGER == null) {
            throw new ShouldNeverHappenException("vGroupMappingManager is NOT init!");
        }
        return ROOT_VGROUP_MAPPING_MANAGER;
    }

    // endregion

    // region get session manager

    /**
     * Gets root session manager.
     *
     * @return the root session manager
     */
    public static SessionManager getRootSessionManager() {
        String group = SeataClusterContext.getGroup();
        return getRootSessionManager(group);
    }

    public static SessionManager getRootSessionManager(String group) {
        return StringUtils.isNotBlank(group) && SESSION_MANAGER_MAP != null
                ? SESSION_MANAGER_MAP.computeIfAbsent(group, k -> ROOT_SESSION_MANAGER)
                : ROOT_SESSION_MANAGER;
    }

    // endregion

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

    /**
     * lock and execute
     *
     * @param globalSession the global session
     * @param lockCallable  the lock Callable
     * @return the value
     */
    public static <T> T lockAndExecute(GlobalSession globalSession, GlobalSession.LockCallable<T> lockCallable)
            throws TransactionException {
        return getRootSessionManager().lockAndExecute(globalSession, lockCallable);
    }

    /**
     * acquire lock
     *
     * @param lockKey the lock key, should be distinct for each lock
     * @return the boolean
     */
    public static boolean acquireDistributedLock(String lockKey) {
        return DISTRIBUTED_LOCKER.acquireLock(
                new DistributedLockDO(lockKey, XID.getIpAddressAndPort(), DISTRIBUTED_LOCK_EXPIRE_TIME));
    }

    /**
     * release lock
     *
     * @return the boolean
     */
    public static boolean releaseDistributedLock(String lockKey) {
        return DISTRIBUTED_LOCKER.releaseLock(
                new DistributedLockDO(lockKey, XID.getIpAddressAndPort(), DISTRIBUTED_LOCK_EXPIRE_TIME));
    }

    /**
     * Execute the function after get the distribute lock
     *
     * @param key  the distribute lock key
     * @param func the function to be call
     * @return whether the func be call
     */
    public static boolean distributedLockAndExecute(String key, NoArgsFunc func) {
        boolean lock = false;
        try {
            lock = acquireDistributedLock(key);
            if (lock) {
                func.call();
            }
        } catch (Exception e) {
            LOGGER.error("Exception running function with key = {}", key, e);
        } finally {
            if (lock) {
                try {
                    SessionHolder.releaseDistributedLock(key);
                } catch (Exception ex) {
                    LOGGER.warn("release distribute lock failure, message = {}", ex.getMessage(), ex);
                }
            }
        }
        return lock;
    }

    public static synchronized void destroy() {
        Throwable failure = null;
        try {
            RaftServerManager.destroy();
        } catch (RuntimeException | Error raftFailure) {
            failure = raftFailure;
        }
        FileStoreRuntime runtime = FILE_STORE_RUNTIME;
        if (runtime != null) {
            try {
                LockerManagerFactory.destroy();
            } catch (RuntimeException | Error facadeFailure) {
                failure = appendFailure(failure, facadeFailure);
            }
            try {
                EnhancedServiceLoader.unload(LockManager.class);
            } catch (RuntimeException | Error unloadFailure) {
                failure = appendFailure(failure, unloadFailure);
            }
            try {
                runtime.close();
            } catch (RuntimeException | Error runtimeFailure) {
                failure = appendFailure(failure, runtimeFailure);
            } finally {
                clearFileModeReferences();
            }
        } else {
            try {
                if (ROOT_SESSION_MANAGER != null) {
                    ROOT_SESSION_MANAGER.destroy();
                }
            } catch (RuntimeException | Error sessionFailure) {
                failure = appendFailure(failure, sessionFailure);
            } finally {
                SESSION_MANAGER_MAP = null;
            }
        }
        rethrow(failure);
    }

    private static Throwable appendFailure(Throwable failure, Throwable next) {
        if (failure == null) {
            return next;
        }
        if (failure != next) {
            failure.addSuppressed(next);
        }
        return failure;
    }

    private static void rethrow(Throwable failure) {
        if (failure instanceof RuntimeException) {
            throw (RuntimeException) failure;
        }
        if (failure instanceof Error) {
            throw (Error) failure;
        }
    }

    @FunctionalInterface
    public interface NoArgsFunc {
        void call();
    }
}
