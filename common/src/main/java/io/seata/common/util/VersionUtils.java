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

import org.apache.commons.lang.StringUtils;

/**
 * The type Version Utils
 *
 * @author wang.liang
 */
public class VersionUtils {

    private VersionUtils() {
    }

    private static final int MAX_PART_SIZE = 5;

    private static final int ONE_PART_LENGTH = 3;

    public static final String UNKNOWN_VERSION = "UNKNOWN";


    /**
     * 将字符串版本号转换为long型版本号
     *
     * @param version 字符串版本号
     * @return long版本号
     * @throws IncompatibleVersionException 不兼容的版本格式
     */
    public static long toLong(String version) throws IncompatibleVersionException {
        if (StringUtils.isBlank(version) || UNKNOWN_VERSION.equalsIgnoreCase(version)) {
            return 0L;
        }

        String[] parts = StringUtils.split(version.replace('_', '.'), '.');
        if (parts.length > MAX_PART_SIZE) {
            throw new IncompatibleVersionException("incompatible version format: " + version);
        }

        long result = 0L;
        int i = 1;

        for (String part : parts) {
            if (StringUtils.isNumeric(part)) {
                result += calculatePartValue(part, i);
            } else {
                String[] subParts = StringUtils.split(part, '-');
                if (StringUtils.isNumeric(subParts[0])) {
                    result += calculatePartValue(subParts[0], i);
                }
            }

            i++;
        }

        if (version.endsWith("-SNAPSHOT")) {
            result = result * 10; // SNAPSHOT版本个位数为0
        } else {
            result = result * 10 + 1; // RELEASE版本个位数为1
        }

        return result;
    }


    //region Private

    /**
     * 计算当前版本号部分的值
     *
     * @param partNumeric 当前部分的版本号字符串
     * @param partIndex   当前部分的index
     * @return 当前版本号部分的值
     */
    private static long calculatePartValue(String partNumeric, int partIndex) {
        if ("0".equals(partNumeric)) {
            return 0;
        } else {
            return Long.parseLong(partNumeric) * Double.valueOf(Math.pow(Math.pow(10, ONE_PART_LENGTH), MAX_PART_SIZE - partIndex)).longValue();
        }
    }

    //endregion
}
