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
package io.seata.tm.api.transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author guoyao
 */
public final class TransactionHookManager {

    private TransactionHookManager() {

    }

    private static final ThreadLocal<List<TransactionHook>> LOCAL_HOOKS = ThreadLocal.withInitial(() -> new ArrayList<>());

    /**
     * get the current hooks
     *
     * @return
     * @throws IllegalStateException
     */
    public static List<TransactionHook> getHooks() throws IllegalStateException {
        List<TransactionHook> hooks = LOCAL_HOOKS.get();
        return Collections.unmodifiableList(hooks);
    }

    /**
     * add new hook
     *
     * @param transactionHook
     */
    public static void registerHook(TransactionHook transactionHook) {
        if (transactionHook == null) {
            throw new NullPointerException("transactionHook must not be null");
        }
        LOCAL_HOOKS.get().add(transactionHook);
    }

    /**
     * clear hooks
     */
    public static void clear() {
        LOCAL_HOOKS.remove();
    }
}
