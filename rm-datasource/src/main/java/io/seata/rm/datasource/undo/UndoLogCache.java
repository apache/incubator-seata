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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author funkye
 */
public class UndoLogCache {

    private static ConcurrentHashMap<String, Object[]> cache = new ConcurrentHashMap<>();

    private static final String DEFAULT_UNDO_LOG_CACHE_KEY_XID_PREFIX = "UNDO_LOG_CACHE_KEY_XID_";

    private static final String DEFAULT_BRANCH_ID_PREFIX = "BRANCH_ID_";

    private static final int XID = 0;

    private static final int BRANCH_ID = 1;

    public static final int CONTEXT = 2;

    public static final int ROLL_BACK_INFO = 3;

    public static final int STATE = 4;

    public static Object[] get(String xid, Long branchId) {
        return cache.get(getCacheKey(xid, branchId));
    }

    public static void remove(String xid, Long branchId) {
        cache.remove(getCacheKey(xid, branchId));
    }

    public static void remove(Set<String> xids) {
        xids.forEach(xid -> {
            cache.forEach((k, v) -> {
                if (k.contains(xid)) {
                    cache.remove(k);
                }
            });
        });
    }

    private static String getCacheKey(String xid, Long branchId) {
        StringBuilder sb = new StringBuilder();
        sb.append(DEFAULT_UNDO_LOG_CACHE_KEY_XID_PREFIX).append(xid).append(DEFAULT_BRANCH_ID_PREFIX).append(branchId);
        return sb.toString();
    }

    public static void put(Object[] objects) {
        cache.put(getCacheKey((String)objects[XID], (Long)objects[BRANCH_ID]), objects);
    }

}
