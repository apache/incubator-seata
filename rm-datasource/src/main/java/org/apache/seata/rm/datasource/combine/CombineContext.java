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
package org.apache.seata.rm.datasource.combine;

import org.apache.seata.core.context.RootContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CombineContext {
    private static final ThreadLocal<Map<String,Boolean>> COMBINE_ASPECT = ThreadLocal.withInitial(ConcurrentHashMap::new);;

    /**
     * @return
     *    false: 指定的key已经存在（重复进入切面）
     *    true: 指定的key不存在（第一次进入切面）
     */
    public static boolean set(){
        String xid = RootContext.getXID();
        if (xid != null) {
            return !Boolean.TRUE.equals(COMBINE_ASPECT.get().putIfAbsent(xid, Boolean.TRUE));
        }
        return false;
    }

    public static boolean get(){
        String xid = RootContext.getXID();
        if (xid == null) {
            return false;
        }
        return Boolean.TRUE.equals(COMBINE_ASPECT.get().get(xid));
    }

    public static void clear(){
        String xid = RootContext.getXID();
        if (xid != null) {
            COMBINE_ASPECT.get().remove(xid);
        }
    }
}
