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
package io.seata.rm.transaction;

import io.seata.common.util.CollectionUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Resource manager transaction hook manager
 *
 * @author wang.liang
 */
public final class RMTransactionHookManager {

    private static final ThreadLocal<List<RMTransactionHook>> GLOBAL_HOOKS = new ThreadLocal<>();

    private RMTransactionHookManager() {
    }

    /**
     * get global hook list
     */
    private static synchronized List<RMTransactionHook> getGlobalHookList() {
        List<RMTransactionHook> transactionHooks = GLOBAL_HOOKS.get();
        if (transactionHooks == null) {
            transactionHooks = new ArrayList<>();
            GLOBAL_HOOKS.set(transactionHooks);
        }
        return transactionHooks;
    }

    /**
     * get the hooks
     */
    public static List<RMTransactionHook> getHooks() {
        List<RMTransactionHook> hooks = GLOBAL_HOOKS.get();
        if (hooks == null || hooks.isEmpty()) {
            return Collections.emptyList();
        }
        return hooks;
    }

    /**
     * add new global hook
     *
     * @param rmTransactionHook
     */
    public static void registerGlobalHook(RMTransactionHook rmTransactionHook) {
        if (rmTransactionHook == null) {
            throw new NullPointerException("RM transactionHook must not be null");
        }
        List<RMTransactionHook> transactionHooks = getGlobalHookList();
        transactionHooks.add(rmTransactionHook);
    }

    /**
     * clear global hooks
     */
    public static void clearGlobalHooks() {
        GLOBAL_HOOKS.remove();
    }

    /**
     * trigger hooks
     *
     * @param logger   the logger in the trigger
     * @param consumer the hook consumer
     */
    public static void triggerHooks(Logger logger, long branchId, Consumer<RMTransactionHook> consumer) {
        List<RMTransactionHook> hooks = getHooks();
        if (CollectionUtils.isNotEmpty(hooks)) {
            for (RMTransactionHook hook : hooks) {
                try {
                    consumer.accept(hook);
                } catch (Exception e) {
                    if (logger != null) {
                        logger.error("execute rm transaction hook failed: branchId=" + branchId, e);
                    }
                }
            }
        }
    }
}
