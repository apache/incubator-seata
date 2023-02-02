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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.seata.common.exception.NotSupportYetException;

/**
 * The type Convert utils.
 *
 * @author wang.liang
 */
public class ConvertUtils {

    private ConvertUtils() {
    }


    @SuppressWarnings("unchecked")
    public static <T> T convert(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isAssignableFrom(value.getClass())) {
            return (T)value;
        }

        if (String.class.equals(targetType)) {
            return (T)String.valueOf(value);
        }

        if (ObjectUtils.isNullOrBlank(value)) {
            // Keep the blank value for List.
            if (List.class.equals(targetType)) {
                return (T)new ArrayList<>();
            }

            return null;
        }

        if (Long.class.equals(targetType)) {
            return (T)Long.valueOf(String.valueOf(value));
        }
        if (Integer.class.equals(targetType)) {
            return (T)Integer.valueOf(String.valueOf(value));
        }
        if (Short.class.equals(targetType)) {
            return (T)Short.valueOf(String.valueOf(value));
        }
        if (Boolean.class.equals(targetType)) {
            return (T)Boolean.valueOf(String.valueOf(value));
        }
        if (Duration.class.equals(targetType)) {
            return (T)Duration.parse(String.valueOf(value));
        }
        if (List.class.equals(targetType)) {
            String str = String.valueOf(value);
            String regex = ",";
            if (str.contains(";")) {
                regex = ";";
            }
            return (T)Arrays.asList(str.split(regex));
        }

        throw new NotSupportYetException("Not support convert to the type: " + targetType.getName());
    }
}
