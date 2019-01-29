/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.core.protocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fescar.common.util.NetUtil;

import io.netty.channel.Channel;

/**
 * The type Version.
 */
public class Version {

    /**
     * The constant CURRENT.
     */
    public static final String CURRENT = "0.1.2";

    /**
     * The constant VERSION_MAP.
     */
    public static final Map<String, String> VERSION_MAP = new ConcurrentHashMap<String, String>();

    private Version() {

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
    public static String checkVersion(String version) throws IncompatibleVersionException {
        // TODO: check
        return version;
    }
}
