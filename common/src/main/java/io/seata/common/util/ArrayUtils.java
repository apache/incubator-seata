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

/**
 * The type Array utils.
 *
 * @author wang.liang
 */
public class ArrayUtils {

    private ArrayUtils() {
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
}
