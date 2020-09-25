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
 * @author wang.liang
 */
public class EnumUtils {
    private EnumUtils() {
    }

    /**
     * Get enum
     *
     * @param enumClass the class
     * @param enumName  the name
     * @param <E>       the type
     * @return the value
     * @throws IllegalArgumentException if the enum has any values, or the name is invalid
     */
    public static <E extends Enum<E>> E getEnum(Class<E> enumClass, String enumName) {
        if (StringUtils.isNotBlank(enumName)) {
            E[] enumValues = enumClass.getEnumConstants();
            if (CollectionUtils.isEmpty(enumValues)) {
                throw new IllegalArgumentException("The enum '" + enumClass.getSimpleName() + "' does not has any values.");
            }

            for (E e : enumValues) {
                if (e.name().equalsIgnoreCase(enumName)) {
                    return e;
                }
            }
        }

        throw new IllegalArgumentException("Unknown enum " + enumClass.getSimpleName() + "[" + enumName + "]");
    }
}
