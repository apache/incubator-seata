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
package io.seata.server.coordinator;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.core.model.BranchType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The type Core factory.
 *
 * @author sharajava
 */
public class CoreFactory {

    private static volatile Map<BranchType, Core> abstractCoreMap =
            new ConcurrentHashMap<BranchType, Core>();

    public static Core getCore(BranchType branchType) {
        if (abstractCoreMap == null) {
            synchronized (CoreFactory.class) {
                if (abstractCoreMap == null) {
                    Map<BranchType, Core> initializedMap = new HashMap<>();
                    List<Core> checkerList = EnhancedServiceLoader.loadAll(Core.class);
                    for (Core checker : checkerList) {
                        initializedMap.put(checker.getBranchType(), checker);
                    }
                    abstractCoreMap = initializedMap;
                }
            }
        }
        if (abstractCoreMap.containsKey(branchType)) {
            return abstractCoreMap.get(branchType);
        }
        throw new NotSupportYetException("");
    }

    private static class SingletonHolder {
        private static Core INSTANCE = new DefaultCore();
    }

    /**
     * Get core.
     *
     * @return the core
     */
    public static final Core get() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Just for test mocking
     *
     * @param core the core
     */
    public static void set(Core core) {
        SingletonHolder.INSTANCE = core;
    }
}
