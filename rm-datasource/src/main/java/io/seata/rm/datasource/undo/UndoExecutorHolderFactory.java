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
package io.seata.rm.datasource.undo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;

/**
 * The Type UndoExecutorHolderFactory
 *
 * @author Zhibei Hao
 */
public class UndoExecutorHolderFactory {

    private static final Map<String, UndoExecutorHolder> UNDO_EXECUTOR_HOLDER_MAP = new ConcurrentHashMap<>();

    /**
     * Get UndoExecutorHolder by db type
     *
     * @param dbType the db type
     * @return the UndoExecutorGroup
     */
    public static UndoExecutorHolder getUndoExecutorHolder(String dbType) {
        return CollectionUtils.computeIfAbsent(UNDO_EXECUTOR_HOLDER_MAP, dbType,
            key -> EnhancedServiceLoader.load(UndoExecutorHolder.class, dbType));
    }
}
