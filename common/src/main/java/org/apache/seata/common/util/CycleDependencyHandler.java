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
package org.apache.seata.common.util;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * The type CycleDependency handler.
 * <p>
 * Used to handle cycle dependencies when converting objects to strings.
 * </p>
 */
public class CycleDependencyHandler {

    private static final ThreadLocal<Set<Object>> OBJECT_SET_LOCAL = new ThreadLocal<>();

    /**
     * Check if cycle dependency handling is starting
     *
     * @return true if starting, false otherwise
     */
    public static boolean isStarting() {
        return OBJECT_SET_LOCAL.get() != null;
    }

    /**
     * Start cycle dependency handling
     */
    public static void start() {
        OBJECT_SET_LOCAL.set(new HashSet<>(8));
    }

    /**
     * End cycle dependency handling
     */
    public static void end() {
        OBJECT_SET_LOCAL.remove();
    }

    /**
     * Add object to the set
     *
     * @param obj the object to add
     */
    public static void addObject(Object obj) {
        if (obj == null) {
            return;
        }

        // get object set
        Set<Object> objectSet = OBJECT_SET_LOCAL.get();
        if (objectSet == null) {
            return;
        }

        // add to object set
        objectSet.add(getUniqueSubstituteObject(obj));
    }

    /**
     * Check if object is contained in the set
     *
     * @param obj the object to check
     * @return true if contained, false otherwise
     */
    public static boolean containsObject(Object obj) {
        if (obj == null) {
            return false;
        }

        // get object set
        Set<Object> objectSet = OBJECT_SET_LOCAL.get();
        if (objectSet == null || objectSet.isEmpty()) {
            return false;
        }

        return objectSet.contains(getUniqueSubstituteObject(obj));
    }

    /**
     * Wrap function with cycle dependency handling
     *
     * @param obj the object
     * @param function the function to apply
     * @param <O> the type of object
     * @return the result of function
     */
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

    /**
     * Convert object to reference string
     *
     * @param obj the object
     * @return the reference string
     */
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
        // Use System.identityHashCode to avoid StackOverflowError
        return System.identityHashCode(obj);
    }
}
