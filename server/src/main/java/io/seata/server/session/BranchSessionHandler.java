package io.seata.server.session;

import io.seata.core.exception.TransactionException;

/**
 * The Functional Interface Branch session handler
 *
 * @author wang.liang
 */
@FunctionalInterface
public interface BranchSessionHandler {

    Boolean CONTINUE = null;

    /**
     * Handle branch session.
     *
     * @param branchSession the branch session
     * @return the handle result
     * @throws TransactionException the transaction exception
     */
    Boolean handle(BranchSession branchSession) throws TransactionException;
}
