package io.seata.server.storage.elasticsearch.session;

import io.seata.common.exception.StoreException;
import io.seata.common.executor.Initialize;
import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.common.util.StringUtils;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.server.session.*;
import io.seata.server.storage.elasticsearch.store.ElasticSearchTransactionStoreManager;
import io.seata.server.store.TransactionStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * @author UmizzZ
 * @date
 */
@LoadLevel(name = "elasticsearch", scope = Scope.PROTOTYPE)
public class ElasticSearchSessionManager extends AbstractSessionManager
    implements Initialize {


    protected static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchSessionManager.class);

    /**
     * The Task name.
     */
    protected String taskName;

    /**
     * Instantiates a new Data base session manager.
     */
    public ElasticSearchSessionManager() {
        super();
    }

    /**
     * Instantiates a new Data base session manager.
     *
     * @param name the name
     */
    public ElasticSearchSessionManager(String name) {
        super();
        this.taskName = name;
    }

    @Override
    public void init() {
        transactionStoreManager = ElasticSearchTransactionStoreManager.getInstance();
    }

    @Override
    public void addGlobalSession(GlobalSession session) throws TransactionException {
        if (StringUtils.isBlank(taskName)) {
            boolean ret = transactionStoreManager.writeSession(TransactionStoreManager.LogOperation.GLOBAL_ADD, session);
            if (!ret) {
                throw new StoreException("addGlobalSession failed.");
            }
        } else {
            boolean ret = transactionStoreManager.writeSession(TransactionStoreManager.LogOperation.GLOBAL_UPDATE, session);
            if (!ret) {
                throw new StoreException("addGlobalSession failed.");
            }
        }
    }

    @Override
    public void updateGlobalSessionStatus(GlobalSession session, GlobalStatus status) throws TransactionException {
        if (StringUtils.isNotBlank(taskName)) {
            return;
        }
        session.setStatus(status);
        boolean ret = transactionStoreManager.writeSession(TransactionStoreManager.LogOperation.GLOBAL_UPDATE, session);
        if (!ret) {
            throw new StoreException("updateGlobalSessionStatus failed.");
        }
    }

    /**
     * remove globalSession
     * 1. rootSessionManager remove normal globalSession
     * 2. retryCommitSessionManager and retryRollbackSessionManager remove retry expired globalSession
     * @param session the session
     * @throws TransactionException
     */
    @Override
    public void removeGlobalSession(GlobalSession session) throws TransactionException {
        boolean ret = transactionStoreManager.writeSession(TransactionStoreManager.LogOperation.GLOBAL_REMOVE, session);
        if (!ret) {
            throw new StoreException("removeGlobalSession failed.");
        }
    }

    @Override
    public void addBranchSession(GlobalSession globalSession, BranchSession session) throws TransactionException {
        if (StringUtils.isNotBlank(taskName)) {
            return;
        }
        boolean ret = transactionStoreManager.writeSession(TransactionStoreManager.LogOperation.BRANCH_ADD, session);
        if (!ret) {
            throw new StoreException("addBranchSession failed.");
        }
    }

    @Override
    public void updateBranchSessionStatus(BranchSession session, BranchStatus status) throws TransactionException {
        if (StringUtils.isNotBlank(taskName)) {
            return;
        }
        boolean ret = transactionStoreManager.writeSession(TransactionStoreManager.LogOperation.BRANCH_UPDATE, session);
        if (!ret) {
            throw new StoreException("updateBranchSessionStatus failed.");
        }
    }

    @Override
    public void removeBranchSession(GlobalSession globalSession, BranchSession session) throws TransactionException {
        if (StringUtils.isNotBlank(taskName)) {
            return;
        }
        boolean ret = transactionStoreManager.writeSession(TransactionStoreManager.LogOperation.BRANCH_REMOVE, session);
        if (!ret) {
            throw new StoreException("removeBranchSession failed.");
        }
    }

    @Override
    public GlobalSession findGlobalSession(String xid) {
        return this.findGlobalSession(xid, true);
    }

    @Override
    public GlobalSession findGlobalSession(String xid, boolean withBranchSessions) {
        return transactionStoreManager.readSession(xid, withBranchSessions);
    }

    @Override
    public Collection<GlobalSession> allSessions() {
        // get by taskName
        if (SessionHolder.ASYNC_COMMITTING_SESSION_MANAGER_NAME.equalsIgnoreCase(taskName)) {
            return findGlobalSessions(new SessionCondition(GlobalStatus.AsyncCommitting));
        } else if (SessionHolder.RETRY_COMMITTING_SESSION_MANAGER_NAME.equalsIgnoreCase(taskName)) {
            return findGlobalSessions(new SessionCondition(new GlobalStatus[] {GlobalStatus.CommitRetrying, GlobalStatus.Committing}));
        } else if (SessionHolder.RETRY_ROLLBACKING_SESSION_MANAGER_NAME.equalsIgnoreCase(taskName)) {
            return findGlobalSessions(new SessionCondition(new GlobalStatus[] {GlobalStatus.RollbackRetrying,
                    GlobalStatus.Rollbacking, GlobalStatus.TimeoutRollbacking, GlobalStatus.TimeoutRollbackRetrying}));
        } else {
            // all data
            return findGlobalSessions(new SessionCondition(new GlobalStatus[] {
                    GlobalStatus.UnKnown, GlobalStatus.Begin,
                    GlobalStatus.Committing, GlobalStatus.CommitRetrying, GlobalStatus.Rollbacking,
                    GlobalStatus.RollbackRetrying,
                    GlobalStatus.TimeoutRollbacking, GlobalStatus.TimeoutRollbackRetrying, GlobalStatus.AsyncCommitting}));
        }
    }

    @Override
    public List<GlobalSession> findGlobalSessions(SessionCondition condition) {
        // nothing need to do
        return transactionStoreManager.readSession(condition);
    }

    @Override
    public <T> T lockAndExecute(GlobalSession globalSession, GlobalSession.LockCallable<T> lockCallable)
            throws TransactionException {
        return lockCallable.call();
    }
}
