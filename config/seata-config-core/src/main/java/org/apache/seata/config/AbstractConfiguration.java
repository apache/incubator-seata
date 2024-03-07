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
package org.apache.seata.config;

import java.time.Duration;

import org.apache.seata.common.util.DurationUtil;
import org.apache.seata.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Abstract configuration.
 *
 */
public abstract class AbstractConfiguration implements Configuration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConfiguration.class);

    /**
     * The constant DEFAULT_CONFIG_TIMEOUT.
     */
    protected static final long DEFAULT_CONFIG_TIMEOUT = 5 * 1000;

    /**
     * The constant DEFAULT_XXX.
     */
    public static final short DEFAULT_SHORT = (short)0;
    public static final int DEFAULT_INT = 0;
    public static final long DEFAULT_LONG = 0L;
    public static final Duration DEFAULT_DURATION = Duration.ZERO;
    public static final boolean DEFAULT_BOOLEAN = false;


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
        return getShort(dataId, DEFAULT_SHORT);
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
        return getInt(dataId, DEFAULT_INT);
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
        return getLong(dataId, DEFAULT_LONG);
    }

    @Override
    public Duration getDuration(String dataId) {
        return getDuration(dataId, DEFAULT_DURATION);
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
        return getBoolean(dataId, DEFAULT_BOOLEAN);
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
        String value = getConfigFromSys(dataId);
        if (value != null) {
            LOGGER.info("Get config from system property, {}={}, type={}", dataId, value, getTypeName());
            return value;
        }

        value = getLatestConfig(dataId, content, timeoutMills);
        LOGGER.info("Get config {}={}, type={}", dataId, value, getTypeName());
        return value;
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
