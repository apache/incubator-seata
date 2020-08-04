/*
 *  Copyright 1999-2020 Seata.io Group.
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
package io.seata.rm.hook;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.rm.AbstractRMHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author JerryYin
 */
public final class GlobalTransactionHookManager {

    private static final Cache<String ,List<GlobalTransactionHook> > LOCAL_HOOKS = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(60*10, TimeUnit.SECONDS)
            .build();

    /**
     * get the current hooks
     *
     * @param xId 由xid组成
     * @return
     * @throws IllegalStateException
     */
    public static List<GlobalTransactionHook> getHooks(String xId) throws IllegalStateException {
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
     * @return return true represents that this session is in global transaction ,otherwise return false.
     * @param globalTransactionHook
     */
    public static boolean registerHook(GlobalTransactionHook globalTransactionHook) {
        if (globalTransactionHook == null) {
            throw new NullPointerException("globalTransactionHook must not be null");
        }
        String xId=RootContext.getXID();
        if(StringUtils.isBlank(xId))
        {
            return false;
        }
        List<GlobalTransactionHook> hookList = LOCAL_HOOKS.getIfPresent(xId);
        if (hookList == null) {
            hookList = new ArrayList<>();
        }
        hookList.add(globalTransactionHook);
        LOCAL_HOOKS.put(xId,hookList);
        return true;
    }

}
