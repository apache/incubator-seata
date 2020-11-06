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
package io.seata.core.protocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;
import io.seata.common.util.NetUtil;
import org.apache.commons.lang.StringUtils;

/**
 * The type Version.
 *
 * @author slievrly
 */
public class Version {

    /**
     * The constant CURRENT.
     */
    private static final String CURRENT = "1.5.0-SNAPSHOT";
    private static final String VERSION_0_7_1 = "0.7.1";
    private static final int MAX_VERSION_DOT = 3;

    /**
     * The constant VERSION_MAP.
     */
    public static final Map<String, String> VERSION_MAP = new ConcurrentHashMap<>();

    private Version() {

    }

    public static String getCurrent() {
        return CURRENT;
    }

    /**
     * Put channel version.
     *
     * @param c the c
     * @param v the v
     */
    public static void putChannelVersion(Channel c, String v) {
        VERSION_MAP.put(NetUtil.toStringAddress(c.remoteAddress()), v);
    }

    /**
     * Gets channel version.
     *
     * @param c the c
     * @return the channel version
     */
    public static String getChannelVersion(Channel c) {
        return VERSION_MAP.get(NetUtil.toStringAddress(c.remoteAddress()));
    }

    /**
     * Check version string.
     *
     * @param version the version
     * @return the string
     * @throws IncompatibleVersionException the incompatible version exception
     */
    public static void checkVersion(String version) throws IncompatibleVersionException {
        long current = convertVersion(CURRENT);
        long clientVersion = convertVersion(version);
        long divideVersion = convertVersion(VERSION_0_7_1);
        if ((current > divideVersion && clientVersion < divideVersion) || (current < divideVersion && clientVersion > divideVersion)) {
            throw new IncompatibleVersionException("incompatible client version:" + version);
        }
    }

    private static long convertVersion(String version) throws IncompatibleVersionException {
        String[] parts = StringUtils.split(version, '.');
        long result = 0L;
        int i = 1;
        int size = parts.length;
        if (size > MAX_VERSION_DOT + 1) {
            throw new IncompatibleVersionException("incompatible version format:" + version);
        }
        size = MAX_VERSION_DOT + 1;
        for (String part : parts) {
            if (StringUtils.isNumeric(part)) {
                result += calculatePartValue(part, size, i);
            } else {
                String[] subParts = StringUtils.split(part, '-');
                if (StringUtils.isNumeric(subParts[0])) {
                    result += calculatePartValue(subParts[0], size, i);
                }
            }

            i++;
        }
        return result;
    }

    private static long calculatePartValue(String partNumeric, int size, int index) {
        return Long.parseLong(partNumeric) * Double.valueOf(Math.pow(100, size - index)).longValue();
    }
}
