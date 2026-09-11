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

import java.lang.reflect.Array;

/**
 * The type Array utils.
 *
 * @author wang.liang
 */
public class ArrayUtils {

    private ArrayUtils() {
    }

    /**
     * arrayObj cast to Object[]
     *
     * @param arrayObj the array obj
     * @return array
     */
    public static Object[] toArray(Object arrayObj) {
        if (arrayObj == null) {
            return null;
        }

        if (!arrayObj.getClass().isArray()) {
            throw new ClassCastException("'arrayObj' is not an array, can't cast to Object[]");
        }

        int length = Array.getLength(arrayObj);
        Object[] array = new Object[length];
        if (length > 0) {
            for (int i = 0; i < length; ++i) {
                array[i] = Array.get(arrayObj, i);
            }
        }
        return array;
    }

    /**
     * Array To String.
     *
     * @param array the array
     * @return str the string
     */
    public static String toString(final Object[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }

        return CycleDependencyHandler.wrap(array, o -> {
            StringBuilder sb = new StringBuilder(32);
            sb.append("[");
            for (Object obj : array) {
                if (sb.length() > 1) {
                    sb.append(", ");
                }
                if (obj == array) {
                    sb.append("(this ").append(obj.getClass().getSimpleName()).append(")");
                } else {
                    sb.append(StringUtils.toString(obj));
                }
            }
            sb.append("]");
            return sb.toString();
        });
    }

    /**
     * Array To String.
     *
     * @param arrayObj the array obj
     * @return str the string
     */
    public static String toString(final Object arrayObj) {
        if (arrayObj == null) {
            return "null";
        }
        if (!arrayObj.getClass().isArray()) {
            return StringUtils.toString(arrayObj);
        }

        if (Array.getLength(arrayObj) == 0) {
            return "[]";
        }

        if (arrayObj.getClass().getComponentType().isPrimitive()) {
            return toString(toArray(arrayObj));
        } else {
            return toString((Object[])arrayObj);
        }
    }
}
