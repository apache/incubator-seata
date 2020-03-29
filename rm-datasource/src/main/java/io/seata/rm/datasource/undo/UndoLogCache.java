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

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author funkye
 */
public class UndoLogCache {

    private static ConcurrentHashMap<String, Object[]> undoLogCache = new ConcurrentHashMap<>();

    private static final String DEFAULT_UNDO_LOG_CACHE_KEY_XID_PREFIX = "UNDO_LOG_CACHE_KEY_XID_";

    private static final String DEFAULT_BRANCHID_PREFIX = "BRANCHID_";

    public static void put(String xid, Long branchId, Object[] objects) {
        undoLogCache.put(getCacheKey(xid, branchId), objects);
    }

    public static Object[] get(String xid, Long branchId) {
        return undoLogCache.get(getCacheKey(xid, branchId));
    }

    public static void remove(String xid, Long branchId) {
        undoLogCache.remove(getCacheKey(xid, branchId));
    }

    private static String getCacheKey(String xid, Long branchId) {
        StringBuffer sb = new StringBuffer();
        sb.append(DEFAULT_UNDO_LOG_CACHE_KEY_XID_PREFIX).append(xid).append(DEFAULT_BRANCHID_PREFIX).append(branchId);
        return sb.toString();
    }
}
