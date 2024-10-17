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
package org.apache.seata.core.protocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;
import org.apache.seata.common.util.NetUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Version.
 *
 */
public class Version {

    private static final Logger LOGGER = LoggerFactory.getLogger(Version.class);

    /**
     * The constant CURRENT.
     */
    private static final String CURRENT = VersionInfo.VERSION;
    private static final String VERSION_0_7_1 = "0.7.1";
    private static final String VERSION_1_5_0 = "1.5.0";
    private static final String VERSION_2_3_0 = "2.3.0";
    private static final int MAX_VERSION_DOT = 3;

    /**
     * The constant VERSION_MAP.
     */
    public static final Map<String, String> VERSION_MAP = new ConcurrentHashMap<>();

    private Version() {

    }

    /**
     * Gets current.
     *
     * @return the current
     */
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
     * Determine whether the client version is greater than or equal to version 1.5.0
     *
     * @param version client version
     * @return true: client version is above or equal version 1.5.0, false: on the contrary
     */
    public static boolean isAboveOrEqualVersion150(String version) {
        return isAboveOrEqualVersion(version, VERSION_1_5_0);
    }

    public static boolean isAboveOrEqualVersion230(String version) {
        return isAboveOrEqualVersion(version, VERSION_2_3_0);
    }

    public static boolean isAboveOrEqualVersion(String clientVersion, String divideVersion) {
        boolean isAboveOrEqualVersion = false;
        try {
            isAboveOrEqualVersion = convertVersion(clientVersion) >= convertVersion(divideVersion);
        } catch (Exception e) {
            LOGGER.error("convert version error, clientVersion:{}", clientVersion, e);
        }
        return isAboveOrEqualVersion;
    }

    public static long convertVersion(String version) throws IncompatibleVersionException {
        if (StringUtils.isBlank(version)) {
            throw new IllegalArgumentException("The version must not be blank.");
        }

        String[] parts = StringUtils.split(version, '.');
        int size = parts.length;
        if (size > MAX_VERSION_DOT + 1) {
            throw new IncompatibleVersionException("incompatible version format:" + version);
        }

        long result = 0L;
        int i = 1;
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

    public static long convertVersionNotThrowException(String version) {
        try {
            return convertVersion(version);
        } catch (Exception e) {
            LOGGER.error("convert version error,version:{}", version, e);
        }
        return -1;
    }

    public static byte calcProtocolVersion(String sdkVersion) throws IncompatibleVersionException {
        long version = convertVersion(sdkVersion);
        long v0 = convertVersion(VERSION_0_7_1);
        if (version <= v0) {
            return ProtocolConstants.VERSION_0;
        } else {
            return ProtocolConstants.VERSION_1;
        }
    }

    private static long calculatePartValue(String partNumeric, int size, int index) {
        return Long.parseLong(partNumeric) * Double.valueOf(Math.pow(100, size - index)).longValue();
    }
}
