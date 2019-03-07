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
package com.alibaba.fescar.tm.api.transaction;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author guoyao
 * @date 2019/3/4
 */
public final class TransactionHookManager {

    private static final ThreadLocal<List<TransactionHook>> hooksLocal=new ThreadLocal<>();

    /**
     * get the current hooks
     *
     * @return
     * @throws IllegalStateException
     */
    public static List<TransactionHook> getHooks() throws IllegalStateException {
        List<TransactionHook> hooks=hooksLocal.get();

        if (hooks == null || hooks.isEmpty()) {
            return Collections.emptyList();
        }
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
        List<TransactionHook> transactionHooks=hooksLocal.get();
        if (transactionHooks == null) {
            hooksLocal.set(new ArrayList<>());
        }
        hooksLocal.get().add(transactionHook);
    }

    public static void clear() {
        hooksLocal.remove();
    }
}
