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

import java.time.Duration;

import io.seata.common.util.DurationUtil;
import io.seata.common.util.StringUtils;

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
    protected static final String ITEM_SPLIT_CHAR = ",";

    /**
     * The constant DEFAULT_VALUE_XXX.
     */
    protected static final short DEFAULT_VALUE_SHORT = (short) 0;
    protected static final int DEFAULT_VALUE_INT = 0;
    protected static final long DEFAULT_VALUE_LONG = 0L;
    protected static final Duration DEFAULT_VALUE_DURATION = Duration.ZERO;
    protected static final boolean DEFAULT_VALUE_BOOLEAN = false;
    protected static final String[] DEFAULT_VALUE_ARRAY = new String[0];

    @Override
    public short getShort(String dataId, short defaultValue, long timeoutMills) {
        String result = getConfig(dataId, timeoutMills);
        return StringUtils.isBlank(result) ? defaultValue : Short.parseShort(result);
    }

    @Override
    public short getShort(String dataId, short defaultValue) {
        return getShort(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    @Override
    public short getShort(String dataId) {
        return getShort(dataId, DEFAULT_VALUE_SHORT);
    }

    @Override
    public int getInt(String dataId, int defaultValue, long timeoutMills) {
        String result = getConfig(dataId, timeoutMills);
        return StringUtils.isBlank(result) ? defaultValue : Integer.parseInt(result);
    }

    @Override
    public int getInt(String dataId, int defaultValue) {
        return getInt(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    @Override
    public int getInt(String dataId) {
        return getInt(dataId, DEFAULT_VALUE_INT);
    }

    @Override
    public long getLong(String dataId, long defaultValue, long timeoutMills) {
        String result = getConfig(dataId, timeoutMills);
        return StringUtils.isBlank(result) ? defaultValue : Long.parseLong(result);
    }

    @Override
    public long getLong(String dataId, long defaultValue) {
        return getLong(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    @Override
    public long getLong(String dataId) {
        return getLong(dataId, DEFAULT_VALUE_LONG);
    }

    @Override
    public Duration getDuration(String dataId) {
        return getDuration(dataId, DEFAULT_VALUE_DURATION);
    }

    @Override
    public Duration getDuration(String dataId, Duration defaultValue) {
        return getDuration(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    @Override
    public Duration getDuration(String dataId, Duration defaultValue, long timeoutMills) {
        String result = getConfig(dataId, timeoutMills);
        return StringUtils.isBlank(result) ? defaultValue : DurationUtil.parse(result);
    }

    @Override
    public boolean getBoolean(String dataId, boolean defaultValue, long timeoutMills) {
        String result = getConfig(dataId, timeoutMills);
        return StringUtils.isBlank(result) ? defaultValue : Boolean.parseBoolean(result);
    }

    @Override
    public boolean getBoolean(String dataId, boolean defaultValue) {
        return getBoolean(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    @Override
    public boolean getBoolean(String dataId) {
        return getBoolean(dataId, DEFAULT_VALUE_BOOLEAN);
    }

    @Override
    public String[] getArray(String dataId, String[] defaultValue, long timeoutMills) {
        String result = getConfig(dataId, timeoutMills);
        return StringUtils.isBlank(result) ? defaultValue : result.split(ITEM_SPLIT_CHAR);
    }

    @Override
    public String[] getArray(String dataId, String[] defaultValue) {
        return getArray(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    @Override
    public String[] getArray(String dataId) {
        return getArray(dataId, DEFAULT_VALUE_ARRAY);
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
    public String getConfig(String dataId, String content, long timeoutMills) {
        return getLatestConfig(dataId, content, timeoutMills);
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
