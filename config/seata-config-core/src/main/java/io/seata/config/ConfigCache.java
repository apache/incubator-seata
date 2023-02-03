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

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.seata.common.util.ConvertUtils;
import io.seata.config.source.ConfigSource;

/**
 * The type ConfigCache.
 *
 * @author wang.liang
 */
public class ConfigCache implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final NonConfigSource NON_CONFIG_SOURCE = new NonConfigSource();


    //region Class

    private final String dataId;

    private final Object value;
    private final String stringValue;
    private final Class<?> type;

    private final ConfigSource source;

    // cache time
    private final Date time = new Date();
    private String timeStr;


    private ConfigCache(@Nonnull String dataId, @Nullable Object value, String stringValue, @Nonnull ConfigSource source) {
        Objects.requireNonNull(dataId, "The dataId must not be null.");
        Objects.requireNonNull(source, "The source must not be null.");

        this.dataId = dataId;
        this.value = value;
        this.stringValue = stringValue;
        this.type = value != null ? value.getClass() : null;
        this.source = source;
    }

    @Nonnull
    public String getDataId() {
        return dataId;
    }

    @Nullable
    public Object getValue() {
        return value;
    }

    @Nullable
    public <T> T getConfig() {
        return (T)value;
    }

    public String getStringValue() {
        return stringValue;
    }

    @Nullable
    public Class<?> getType() {
        return type;
    }

    @Nonnull
    public ConfigSource getSource() {
        return source;
    }

    @Nonnull
    public String getSourceName() {
        return source.getName();
    }

    @Nonnull
    public Date getTime() {
        return time;
    }


    @Nonnull
    public String getTimeStr() {
        if (timeStr == null) {
            timeStr = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS").format(new Date());
        }

        return timeStr;
    }

    //endregion Class


    //region static

    @Nonnull
    public static ConfigCache create(String dataId, Object value, String stringValue, ConfigSource source) {
        if (value == null && source == null) {
            source = NON_CONFIG_SOURCE;
        }

        return new ConfigCache(dataId, value, stringValue, source);
    }

    //endregion static


    @Override
    public String toString() {
        return '[' +
                "c=" + value +
                (type != null ? (", t=" + type.getSimpleName()) : "") +
                ", s=" + getSourceName() +
                ']';
    }


    public static ConfigCache fromConfigInfo(String dataId, ConfigInfo config, Class<?> dataType) {
        Object value = null;
        String stringValue = null;
        ConfigSource source = null;
        if (config != null) {
            value = ConvertUtils.convert(config.getValue(), dataType);
            stringValue = config.getValue();
            source = config.getSource();
        }

        // Wrap config, also when config is null or blank
        return create(dataId, value, stringValue, source);
    }


    private static class NonConfigSource implements ConfigSource {
        @Override
        public String getLatestConfig(String dataId, long timeoutMills) {
            return null;
        }

        @Nonnull
        @Override
        public String getName() {
            return "non";
        }
    }
}
