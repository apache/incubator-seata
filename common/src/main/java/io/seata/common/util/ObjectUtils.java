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
import java.util.Collection;
import java.util.Map;

/**
 * The type Object utils.
 *
 * @author wang.liang
 */
public class ObjectUtils {

    private ObjectUtils() {
    }


    public static boolean isNullOrBlank(Object obj) {
        if (obj == null) {
            return true;
        }

        Class<?> clazz = obj.getClass();

        if (CharSequence.class.isAssignableFrom(clazz)) {
            return StringUtils.isBlank(String.valueOf(obj));
        }
        if (Character.class.isAssignableFrom(clazz)) {
            return Character.isWhitespace((Character)obj);
        }
        if (Map.class.isAssignableFrom(clazz)) {
            return ((Map<?, ?>)obj).isEmpty();
        }
        if (Collection.class.isAssignableFrom(clazz)) {
            return ((Collection<?>)obj).isEmpty();
        }
        if (clazz.isArray()) {
            return Array.getLength(obj) == 0;
        }

        return StringUtils.isBlank(String.valueOf(obj));
    }
}
