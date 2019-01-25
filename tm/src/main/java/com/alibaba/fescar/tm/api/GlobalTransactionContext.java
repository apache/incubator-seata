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

/**
 * Context of global transaction on current thread.
 */
public class GlobalTransactionContext {

    private static final ThreadLocal<GlobalTransaction> THREAD_TRANSACTION_CONTEXT = new ThreadLocal<>();

    private GlobalTransactionContext() {
    }

    /**
     * Try to create a new GlobalTransaction.
     * @return
     */
    private static GlobalTransaction createNew() {
        GlobalTransaction tx = new DefaultGlobalTransaction();
        THREAD_TRANSACTION_CONTEXT.set(tx);
        return THREAD_TRANSACTION_CONTEXT.get();
    }

    /**
     * Get GlobalTransaction instance bind on current thread.
     *
     * @return null if no transaction context there.
     */
    public static GlobalTransaction getCurrent() {
        GlobalTransaction tx = THREAD_TRANSACTION_CONTEXT.get();
        if (tx != null) {
            return tx;
        }
        String xid = RootContext.getXID();
        if (xid == null) {
            return null;
        }
        tx = new DefaultGlobalTransaction(xid);
        THREAD_TRANSACTION_CONTEXT.set(tx);
        return THREAD_TRANSACTION_CONTEXT.get();
    }

    /**
     * Get GlobalTransaction instance bind on current thread.
     * Create a new on if no existing there.
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
     * Clean context.
     */
    public static void clean() {
        THREAD_TRANSACTION_CONTEXT.remove();
        RootContext.unbind();
    }
}
