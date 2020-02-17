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
package io.seata.rm;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.seata.core.context.RootContext;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author guoyao
 */
public final class GlobalTransactionHookManager {

    private static final Cache<String ,List<GlobalTransactionHook> > LOCAL_HOOKS = CacheBuilder.newBuilder()
            .maximumSize(2000)
            .expireAfterWrite(60*10, TimeUnit.SECONDS)
            .build();

    /**
     * get the current hooks
     *
     * @param xId 由xid组成
     * @return
     * @throws IllegalStateException
     */
    public static List<GlobalTransactionHook> popHooks(String xId) throws IllegalStateException {
        List<GlobalTransactionHook> hooks = LOCAL_HOOKS.getIfPresent(xId);
        if (hooks == null) {
            return Collections.emptyList();
        }
        else {
            LOCAL_HOOKS.invalidate(xId);
        }
        return Collections.unmodifiableList(hooks);
    }

    /**
     * add new hook
     *
     * @param globalTransactionHook
     */
    public static void registerHook(GlobalTransactionHook globalTransactionHook) {
        if (globalTransactionHook == null) {
            throw new NullPointerException("globalTransactionHook must not be null");
        }
        String xId=RootContext.getXID();
        List<GlobalTransactionHook> hookList = LOCAL_HOOKS.getIfPresent(xId);
        if (hookList == null) {
            hookList = new ArrayList<>();
        }
        hookList.add(globalTransactionHook);
        LOCAL_HOOKS.put(xId,hookList);
    }

}
