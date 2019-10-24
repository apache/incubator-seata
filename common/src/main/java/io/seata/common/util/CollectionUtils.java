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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The type Collection utils.
 *
 * @author zhangsen
 * @author Geng Zhang
 */
public class CollectionUtils {

    /**
     * Is empty boolean.
     *
     * @param col the col
     * @return the boolean
     */
    public static boolean isEmpty(Collection col) {
        return !isNotEmpty(col);
    }

    /**
     * Is not empty boolean.
     *
     * @param col the col
     * @return the boolean
     */
    public static boolean isNotEmpty(Collection col) {
        return col != null && col.size() > 0;
    }

    /**
     * Is empty boolean.
     *
     * @param array the array
     * @return the boolean
     */
    public static boolean isEmpty(Object[] array) {
        return !isNotEmpty(array);
    }

    /**
     * Is not empty boolean.
     *
     * @param array the array
     * @return the boolean
     */
    public static boolean isNotEmpty(Object[] array) {
        return array != null && array.length > 0;
    }

    /**
     * To string string.
     *
     * @param col the col
     * @return the string
     */
    public static String toString(Collection col) {
        if (isEmpty(col)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Object obj : col) {
            sb.append(StringUtils.toString(obj));
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }

    /**
     * Is size equals boolean.
     *
     * @param col0 the col 0
     * @param col1 the col 1
     * @return the boolean
     */
    public static boolean isSizeEquals(Collection col0, Collection col1) {
        if (col0 == null) {
            return col1 == null;
        } else {
            if (col1 == null) {
                return false;
            } else {
                return col0.size() == col1.size();
            }
        }
    }

    private static final String KV_SPLIT = "=";

    private static final String PAIR_SPLIT = "&";

    /**
     * Encode map to string
     *
     * @param map origin map
     * @return String string
     */
    public static String encodeMap(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        if (map.isEmpty()) {
            return StringUtils.EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(entry.getKey()).append(KV_SPLIT).append(entry.getValue()).append(PAIR_SPLIT);
        }
        return sb.substring(0, sb.length() - 1);
    }

    /**
     * Decode string to map
     *
     * @param data data
     * @return map map
     */
    public static Map<String, String> decodeMap(String data) {
        if (data == null) {
            return null;
        }
        Map<String, String> map = new ConcurrentHashMap<>();
        if (StringUtils.isBlank(data)) {
            return map;
        }
        String[] kvPairs = data.split(PAIR_SPLIT);
        if (kvPairs.length == 0) {
            return map;
        }
        for (String kvPair : kvPairs) {
            if (StringUtils.isNullOrEmpty(kvPair)) {
                continue;
            }
            String[] kvs = kvPair.split(KV_SPLIT);
            if (kvs.length != 2) {
                continue;
            }
            map.put(kvs[0], kvs[1]);
        }
        return map;
    }

    /**
     * To upper list list.
     *
     * @param sourceList the source list
     * @return the list
     */
    public static List<String> toUpperList(List<String> sourceList) {
        if (null == sourceList || sourceList.size() == 0) { return sourceList; }
        List<String> destList = new ArrayList<>(sourceList.size());
        for (String element : sourceList) {
            if (null != element) {
                destList.add(element.toUpperCase());
            } else {
                destList.add(element);
            }
        }
        return destList;
    }
}
