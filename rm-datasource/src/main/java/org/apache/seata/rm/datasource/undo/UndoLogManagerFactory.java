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
package org.apache.seata.rm.datasource.undo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.util.CollectionUtils;


public class UndoLogManagerFactory {

    private static final Map<String, UndoLogManager> UNDO_LOG_MANAGER_MAP = new ConcurrentHashMap<>();

    /**
     * get undo log manager.
     *
     * @param dbType the db type
     * @return undo log manager.
     */
    public static UndoLogManager getUndoLogManager(String dbType) {
        return CollectionUtils.computeIfAbsent(UNDO_LOG_MANAGER_MAP, dbType,
            key -> EnhancedServiceLoader.load(UndoLogManager.class, dbType));
    }
}
