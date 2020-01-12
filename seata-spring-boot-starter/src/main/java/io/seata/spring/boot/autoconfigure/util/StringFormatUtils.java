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
package io.seata.spring.boot.autoconfigure.util;

/**
 * @author xingfudeshi@gmail.com
 */
public class StringFormatUtils {
    private static final char MINUS = '-';
    private static final char UNDERLINE = '_';
    private static final char DOT = '.';

    /**
     * camelTo underline format
     *
     * @param param
     * @return formatted string
     */
    public static String camelToUnderline(String param) {
        if (param == null || "".equals(param.trim())) {
            return "";
        }
        int len = param.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append(UNDERLINE);
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * underline to camel
     *
     * @param param
     * @return formatted string
     */
    public static String underlineToCamel(String param) {
        return formatCamel(param, UNDERLINE);
    }

    /**
     * minus to camel
     *
     * @param param
     * @return formatted string
     */
    public static String minusToCamel(String param) {
        return formatCamel(param, MINUS);
    }

    /**
     * dot to camel
     *
     * @param param
     * @return formatted string
     */
    public static String dotToCamel(String param) {
        return formatCamel(param, DOT);
    }

    /**
     * format camel
     *
     * @param param
     * @param sign
     * @return formatted string
     */
    private static String formatCamel(String param, char sign) {
        if (param == null || "".equals(param.trim())) {
            return "";
        }
        int len = param.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            if (c == sign) {
                if (++i < len) {
                    sb.append(Character.toUpperCase(param.charAt(i)));
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }


}
