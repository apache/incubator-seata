/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.integration.tx.api.fence.hook;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TccHookManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TccHookManager.class);

    private TccHookManager() {

    }

    private static final List<TccHook> TCC_HOOKS = new CopyOnWriteArrayList<>();
    // Cache unmodifiable lists
    private volatile static List<TccHook> CACHED_UNMODIFIABLE_HOOKS = null;

    /**
     * get the hooks
     * @return tccHook list
     */
    public static List<TccHook> getHooks() {
        if (CACHED_UNMODIFIABLE_HOOKS == null) {
            synchronized (TccHookManager.class) {
                if (CACHED_UNMODIFIABLE_HOOKS == null) {
                    CACHED_UNMODIFIABLE_HOOKS = Collections.unmodifiableList(TCC_HOOKS);
                }
            }
        }
        return CACHED_UNMODIFIABLE_HOOKS;
    }

    /**
     * add new hook
     * @param tccHook tccHook
     */
    public static void registerHook(TccHook tccHook) {
        if (tccHook == null) {
            throw new NullPointerException("tccHook must not be null");
        }
        TCC_HOOKS.add(tccHook);
        CACHED_UNMODIFIABLE_HOOKS = null;
        LOGGER.info("TccHook registered succeeded! TccHooks size: {}", TCC_HOOKS.size());
    }

    /**
     * clear hooks
     */
    public static void clear() {
        TCC_HOOKS.clear();
        CACHED_UNMODIFIABLE_HOOKS = null;
        LOGGER.info("All TccHooks have been cleared.");
    }
}