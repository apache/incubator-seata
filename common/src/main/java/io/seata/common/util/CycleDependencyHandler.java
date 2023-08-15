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

    private static final ThreadLocal<Set<Object>> OBJECT_SET_LOCAL = new ThreadLocal<>();

    public static boolean isStarting() {
        return OBJECT_SET_LOCAL.get() != null;
    }

    public static void start() {
        OBJECT_SET_LOCAL.set(new HashSet<>(8));
    }

    public static void end() {
        OBJECT_SET_LOCAL.remove();
    }

    public static void addObject(Object obj) {
        if (obj == null) {
            return;
        }

        // get object set
        Set<Object> objectSet = OBJECT_SET_LOCAL.get();

        // add to object set
        objectSet.add(getUniqueSubstituteObject(obj));
    }

    public static boolean containsObject(Object obj) {
        if (obj == null) {
            return false;
        }

        // get object set
        Set<Object> objectSet = OBJECT_SET_LOCAL.get();
        if (objectSet.isEmpty()) {
            return false;
        }

        return objectSet.contains(getUniqueSubstituteObject(obj));
    }

    public static <O> String wrap(O obj, Function<O, String> function) {
        boolean isStarting = isStarting();
        try {
            if (!isStarting) {
                start();
            } else {
                if (containsObject(obj)) {
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

    public static String toRefString(Object obj) {
        return "(ref " + obj.getClass().getSimpleName() + ")";
    }

    /**
     * get Unique Substitute Object.
     * Avoid `obj.hashCode()` throwing `StackOverflowError` during cycle dependency.
     *
     * @param obj the object
     * @return the substitute object
     */
    private static Object getUniqueSubstituteObject(Object obj) {
        // TODO: HELP-WANTED: Optimize this method to ensure uniqueness
        return System.identityHashCode(obj);
    }
}
