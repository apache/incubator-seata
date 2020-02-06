package io.seata.server.coordinator;

import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;

/**
 * @author zhangchenghui.dev@gmail.com
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
