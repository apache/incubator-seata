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
package io.seata.config;


import io.seata.common.util.DurationUtil;

import java.time.Duration;

/**
 * The type Abstract configuration.
 *
 * @author slievrly
 */
public abstract class AbstractConfiguration implements Configuration {

    /**
     * The constant DEFAULT_CONFIG_TIMEOUT.
     */
    protected static final long DEFAULT_CONFIG_TIMEOUT = 5 * 1000;

    @Override
    public short getShort(String dataId, int defaultValue, long timeoutMills) {
        String result = getConfig(dataId, String.valueOf(defaultValue), timeoutMills);
        return Short.parseShort(result);
    }

    @Override
    public short getShort(String dataId, short defaultValue) {
        return getShort(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    @Override
    public short getShort(String dataId) {
        return getShort(dataId, (short) 0);
    }

    @Override
    public int getInt(String dataId, int defaultValue, long timeoutMills) {
        String result = getConfig(dataId, String.valueOf(defaultValue), timeoutMills);
        return Integer.parseInt(result);
    }

    @Override
    public int getInt(String dataId, int defaultValue) {
        return getInt(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    @Override
    public int getInt(String dataId) {
        return getInt(dataId, 0);
    }

    @Override
    public long getLong(String dataId, long defaultValue, long timeoutMills) {
        String result = getConfig(dataId, String.valueOf(defaultValue), timeoutMills);
        return Long.parseLong(result);
    }

    @Override
    public long getLong(String dataId, long defaultValue) {
        return getLong(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    @Override
    public long getLong(String dataId) {
        return getLong(dataId, 0L);
    }

    @Override
    public Duration getDuration(String dataId) {
        return getDuration(dataId, Duration.ZERO);
    }

    @Override
    public Duration getDuration(String dataId, Duration defaultValue) {
        return getDuration(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    @Override
    public Duration getDuration(String dataId, Duration defaultValue, long timeoutMills) {
        String result = getConfig(dataId, defaultValue.toMillis() + "ms", timeoutMills);
        return DurationUtil.parse(result);
    }

    @Override
    public boolean getBoolean(String dataId, boolean defaultValue, long timeoutMills) {
        String result = getConfig(dataId, String.valueOf(defaultValue), timeoutMills);
        return Boolean.parseBoolean(result);
    }

    @Override
    public boolean getBoolean(String dataId, boolean defaultValue) {
        return getBoolean(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    @Override
    public boolean getBoolean(String dataId) {
        return getBoolean(dataId, false);
    }

    @Override
    public String getConfig(String dataId, String defaultValue) {
        return getConfig(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    @Override
    public String getConfig(String dataId, long timeoutMills) {
        return getConfig(dataId, null, timeoutMills);
    }

    @Override
    public String getConfig(String dataId) {
        return getConfig(dataId, DEFAULT_CONFIG_TIMEOUT);
    }

    @Override
    public boolean putConfig(String dataId, String content) {
        return putConfig(dataId, content, DEFAULT_CONFIG_TIMEOUT);
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content) {
        return putConfigIfAbsent(dataId, content, DEFAULT_CONFIG_TIMEOUT);
    }

    @Override
    public boolean removeConfig(String dataId) {
        return removeConfig(dataId, DEFAULT_CONFIG_TIMEOUT);
    }

    /**
     * Gets type name.
     *
     * @return the type name
     */
    public abstract String getTypeName();
}
