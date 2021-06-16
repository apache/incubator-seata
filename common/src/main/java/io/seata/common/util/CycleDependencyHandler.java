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
package io.seata.common.util;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * The type CycleDependency handler.
 *
 * @author wang.liang
 */
public class CycleDependencyHandler {

    private static final ThreadLocal<Set<Object>> OBJECT_UNIQUE_CODE_SET_LOCAL = new ThreadLocal<>();

    public static boolean isStarting() {
        return OBJECT_UNIQUE_CODE_SET_LOCAL.get() != null;
    }

    public static void start() {
        OBJECT_UNIQUE_CODE_SET_LOCAL.set(new HashSet<>(8));
    }

    public static void end() {
        OBJECT_UNIQUE_CODE_SET_LOCAL.remove();
    }

    public static void addObject(Object obj) {
        if (obj == null) {
            return;
        }

        // get object unique code set
        Set<Object> objectUniqueCodeSet = OBJECT_UNIQUE_CODE_SET_LOCAL.get();

        // add to object unique code set
        objectUniqueCodeSet.add(getObjectUniqueCode(obj));
    }

    public static boolean hasObject(Object obj) {
        if (obj == null) {
            return false;
        }

        // get object unique code set
        Set<Object> objectUniqueCodeSet = OBJECT_UNIQUE_CODE_SET_LOCAL.get();
        if (objectUniqueCodeSet.isEmpty()) {
            return false;
        }

        return objectUniqueCodeSet.contains(getObjectUniqueCode(obj));
    }

    public static <O> String wrap(O obj, Function<O, String> function) {
        boolean isStarting = CycleDependencyHandler.isStarting();
        try {
            if (!isStarting) {
                start();
            } else {
                if (hasObject(obj)) {
                    return toRefString(obj);
                }
            }

            // add object
            addObject(obj);

            // do function
            return function.apply(obj);
        } finally {
            if (!isStarting) {
                end();
            }
        }
    }

    /**
     * get object unique code.
     * Avoid throwing 'StackOverflowError' by 'Collection' and 'Map' during cycle dependency.
     *
     * @param obj
     * @return
     */
    private static Object getObjectUniqueCode(Object obj) {
        return System.identityHashCode(obj);
    }

    public static String toRefString(Object obj) {
        return "(ref " + obj.getClass().getSimpleName() + ")";
    }
}
