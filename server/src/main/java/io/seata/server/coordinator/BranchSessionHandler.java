package io.seata.server.coordinator;

import io.seata.core.exception.TransactionException;
import io.seata.server.session.BranchSession;

/**
 * The Functional Interface Branch session handler
 *
 * @author wang.liang
 */
@FunctionalInterface
public interface BranchSessionHandler {

    /**
     * Handle branch session.
     *
     * @param branchSession the branch session
     * @return the handle result
     * @throws TransactionException the transaction exception
     */
    Boolean handle(BranchSession branchSession) throws TransactionException;
}
