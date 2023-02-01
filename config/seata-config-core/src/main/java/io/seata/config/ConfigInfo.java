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

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.Nonnull;

import io.seata.common.util.ObjectUtils;
import io.seata.config.source.ConfigSource;

/**
 * The type ConfigInfo.
 *
 * @author wang.liang
 */
public class ConfigInfo<T> {

    private final T value;
    private final String stringValue;

    private final ConfigSource fromSource;

    private final Date time = new Date();
    private String timeStr;


    public ConfigInfo(T value, ConfigSource fromSource) {
        if (ObjectUtils.isNullOrBlank(value)) {
            throw new IllegalArgumentException("The config value must be not null or blank.");
        }

        this.value = value;
        this.stringValue = String.valueOf(value);
        this.fromSource = fromSource;
    }


    @Nonnull
    public T getValue() {
        return value;
    }

    @Nonnull
    public String getStringValue() {
        return stringValue;
    }

    public ConfigSource getFromSource() {
        return fromSource;
    }

    public String getFromSourceTypeName() {
        return fromSource != null ? fromSource.getTypeName() : null;
    }

    @Nonnull
    public Date getTime() {
        return time;
    }

    @Nonnull
    public String getStringTime() {
        if (timeStr == null) {
            timeStr = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS").format(new Date());
        }

        return timeStr;
    }


    @Override
    public String toString() {
        return '{' +
                "v=" + stringValue +
                ", s=" + getFromSourceTypeName() +
                ", t=" + getStringTime() +
                '}';
    }
}
