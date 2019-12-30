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
package io.seata.tm.api;

import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.tm.api.transaction.TransactionInfo;

/**
 * GlobalTransaction API
 *
 * @author sharajava
 */
public class GlobalTransactionContext {

    private GlobalTransactionContext() {
    }

    /**
     * Try to create a new GlobalTransaction.
     *
     * @return
     */
    private static GlobalTransaction createNew() {
        GlobalTransaction tx = new DefaultGlobalTransaction();
        return tx;
    }

    /**
     * Get GlobalTransaction instance bind on current thread.
     *
     * @return null if no transaction context there.
     */
    private static GlobalTransaction getCurrent(Propagation propagation) {
        String xid = RootContext.getXID();
        String xidRole = RootContext.getXIDRole();
        if (xid == null && xidRole == null) {
            return null;
        }
        //has an external transaction
        else {
            GlobalTransactionRole role = getTransactionRole(propagation);
            //the propagation is RequiresNew
            if (role == GlobalTransactionRole.Launcher) {
                String previousXid = xid;
                GlobalTransactionRole previousRole = GlobalTransactionRole.get(xidRole);
                return new DefaultGlobalTransaction(null, GlobalStatus.Begin, GlobalTransactionRole.Launcher, previousXid
                        , previousRole);
            }
            else
                //the propagation is NotSupported
                if (role == GlobalTransactionRole.Excluded) {
                    String previousXid = xid;
                    GlobalTransactionRole previousRole = GlobalTransactionRole.get(xidRole);
                    return new DefaultGlobalTransaction(null, GlobalStatus.Begin, GlobalTransactionRole.Excluded, previousXid
                            , previousRole);
                }
                //the propagation is Required
                else {
                    return new DefaultGlobalTransaction(xid, GlobalStatus.Begin, GlobalTransactionRole.Participant);
                }
        }

    }

    /**
     * Get GlobalTransaction instance bind on current thread. Create a new on if no existing there.
     *
     * @return new context if no existing there.
     */
    public static GlobalTransaction getCurrentOrCreate(TransactionInfo txInfo) {
        Propagation propagation = txInfo.getPropagation();
        GlobalTransaction tx = getCurrent(propagation);
        if (tx == null) {
            if (propagation == Propagation.NOT_SUPPORTED) {
                return new DefaultGlobalTransaction(null, GlobalStatus.UnKnown, GlobalTransactionRole.Excluded);
            }
            return createNew();

        }
        return tx;
    }

    /**
     * Reload GlobalTransaction instance according to the given XID
     *
     * @param xid the xid
     * @return reloaded transaction instance.
     * @throws TransactionException the transaction exception
     */
    public static GlobalTransaction reload(String xid) throws TransactionException {
        GlobalTransaction tx = new DefaultGlobalTransaction(xid, GlobalStatus.UnKnown, GlobalTransactionRole.Launcher) {
            @Override
            public void begin(int timeout, String name) throws TransactionException {
                throw new IllegalStateException("Never BEGIN on a RELOADED GlobalTransaction. ");
            }
        };
        return tx;
    }

    /**
     * Get TransactionRole
     *
     * @param propagation
     * @return
     */
    private static GlobalTransactionRole getTransactionRole(Propagation propagation) {
        switch (propagation) {
            case REQUIRED:
                return GlobalTransactionRole.Participant;
            case REQUIRES_NEW:
                return GlobalTransactionRole.Launcher;
            case NOT_SUPPORTED:
                return GlobalTransactionRole.Excluded;
            default:
                throw new IllegalStateException("Unsupported transaction propagation strategy.");
        }
    }
}
