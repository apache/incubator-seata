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
package io.seata.common.util;


import java.io.InputStream;
import java.util.Iterator;

/**
 * The type String utils.
 *
 * Compatible for dubbo dubbo-filter-seata
 * Notes:
 * https://github.com/apache/dubbo-spi-extensions/blob/master/dubbo-filter-extensions/dubbo-filter-seata/src/main
 * /java/org/apache/dubbo/seata/SeataTransactionPropagationProviderFilter.java
 */
@Deprecated
public class StringUtils {
    /**
     * Is empty boolean.
     *
     * @param str the str
     * @return the boolean
     */
    public static boolean isNullOrEmpty(String str) {
        return org.apache.seata.common.util.StringUtils.isNullOrEmpty(str);
    }

    /**
     * Is blank string ?
     *
     * @param str the str
     * @return boolean boolean
     */
    public static boolean isBlank(String str) {
        return org.apache.seata.common.util.StringUtils.isBlank(str);
    }

    /**
     * Is Not blank string ?
     *
     * @param str the str
     * @return boolean boolean
     */
    public static boolean isNotBlank(String str) {
        return org.apache.seata.common.util.StringUtils.isNotBlank(str);
    }

    /**
     * Equals boolean.
     *
     * @param a the a
     * @param b the b
     * @return boolean
     */
    public static boolean equals(String a, String b) {
        return org.apache.seata.common.util.StringUtils.equals(a,b);
    }

    /**
     * Equals ignore case boolean.
     *
     * @param a the a
     * @param b the b
     * @return the boolean
     */
    public static boolean equalsIgnoreCase(String a, String b) {
        return org.apache.seata.common.util.StringUtils.equalsIgnoreCase(a,b);
    }

    /**
     * Input stream 2 string string.
     *
     * @param is the is
     * @return the string
     */
    public static String inputStream2String(InputStream is) {
        return org.apache.seata.common.util.StringUtils.inputStream2String(is);
    }

    /**
     * Input stream to byte array
     *
     * @param is the is
     * @return the byte array
     */
    public static byte[] inputStream2Bytes(InputStream is) {
        return org.apache.seata.common.util.StringUtils.inputStream2Bytes(is);
    }

    /**
     * Object.toString()
     *
     * @param obj the obj
     * @return string string
     */
    @SuppressWarnings("deprecation")
    public static String toString(final Object obj) {
        return org.apache.seata.common.util.StringUtils.toString(obj);
    }

    /**
     * Trim string to null if empty("").
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed String
     */
    public static String trimToNull(final String str) {
        return org.apache.seata.common.util.StringUtils.trimToNull(str);
    }

    /**
     * Trim string, or null if string is null.
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed string, {@code null} if null String input
     */
    public static String trim(final String str) {
        return org.apache.seata.common.util.StringUtils.trim(str);
    }

    /**
     * Checks if a CharSequence is empty ("") or null.
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     */
    public static boolean isEmpty(final CharSequence cs) {
        return org.apache.seata.common.util.StringUtils.isEmpty(cs);
    }

    /**
     * Checks if a CharSequence is not empty ("") and not null.
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is not empty and not null
     */
    public static boolean isNotEmpty(final CharSequence cs) {
        return org.apache.seata.common.util.StringUtils.isNotEmpty(cs);
    }

    /**
     * hump to Line or line to hump, only spring environment use
     *
     * @param str str
     * @return string string
     */
    public static String hump2Line(String str) {
        return org.apache.seata.common.util.StringUtils.hump2Line(str);
    }

    /**
     * check string data size
     *
     * @param data the str
     * @param dataName the data name
     * @param errorSize throw exception if size > errorSize
     * @return boolean
     */
    public static boolean checkDataSize(String data, String dataName, int errorSize, boolean throwIfErr) {
        return org.apache.seata.common.util.StringUtils.checkDataSize(data,dataName,errorSize,throwIfErr);
    }

    public static boolean hasLowerCase(String str) {
        return org.apache.seata.common.util.StringUtils.hasLowerCase(str);
    }

    public static boolean hasUpperCase(String str) {
        return org.apache.seata.common.util.StringUtils.hasUpperCase(str);
    }

    public static String join(Iterator iterator, String separator) {
        return org.apache.seata.common.util.StringUtils.join(iterator,separator);
    }

}

