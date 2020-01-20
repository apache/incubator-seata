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

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.loader.EnhancedServiceLoader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jsbxyyx
 */
public class UndoLogManagerFactory {

    private static volatile Map<String, UndoLogManager> undoLogManagerMap;

    /**
     * get undo log manager.
     *
     * @param dbType the db type
     * @return undo log manager.
     */
    public static UndoLogManager getUndoLogManager(String dbType) {
        if (undoLogManagerMap == null) {
            synchronized (UndoLogManagerFactory.class) {
                if (undoLogManagerMap == null) {
                    Map<String, UndoLogManager> initializedMap = new HashMap<>();
                    List<UndoLogManager> undoLogList = EnhancedServiceLoader.loadAll(UndoLogManager.class);
                    for (UndoLogManager undoLog : undoLogList) {
                        initializedMap.put(undoLog.getDbType(), undoLog);
                    }
                    undoLogManagerMap = initializedMap;
                }
            }
        }
        if (undoLogManagerMap.containsKey(dbType)) {
            return undoLogManagerMap.get(dbType);
        }
        throw new NotSupportYetException("not support dbType[" + dbType + "]");
    }

}
