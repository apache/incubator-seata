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

package com.alibaba.fescar.tm.api;

import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.GlobalStatus;
import com.alibaba.fescar.core.model.TransactionManager;

/**
 * GlobalTransaction API
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
    private static GlobalTransaction getCurrent() {
        String xid = RootContext.getXID();
        if (xid == null) {
            return null;
        }
        return new DefaultGlobalTransaction(xid, GlobalStatus.Begin, GlobalTransactionRole.Participant);
    }

    /**
     * Get GlobalTransaction instance bind on current thread. Create a new on if no existing there.
     *
     * @return new context if no existing there.
     */
    public static GlobalTransaction getCurrentOrCreate() {
        GlobalTransaction tx = getCurrent();
        if (tx == null) {
            return createNew();
        }
        return tx;
    }

    /**
     * Reload GlobalTransaction instance according to the given XID
     *
     * @return reloaded transaction instance.
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
}
