package io.seata.server.session;

import io.seata.core.exception.TransactionException;

/**
 * The Functional Interface Global session handler
 *
 * @author wang.liang
 */
@FunctionalInterface
public interface GlobalSessionHandler {

    /**
     * Handle global session.
     *
     * @param globalSession the global session
     * @throws TransactionException the transaction exception
     */
    void handle(GlobalSession globalSession) throws TransactionException;
}