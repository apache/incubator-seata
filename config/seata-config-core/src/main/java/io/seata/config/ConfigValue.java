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

import com.sun.istack.internal.NotNull;
import io.seata.common.util.ObjectUtils;
import io.seata.config.source.ConfigurationSource;

/**
 * The type ConfigValue.
 *
 * @author wang.liang
 */
public class ConfigValue<T> {

    private final T value;
    private final String stringValue;
    private final ConfigurationSource fromSource;
    private final Date time = new Date();


    public ConfigValue(T value, ConfigurationSource fromSource) {
        if (ObjectUtils.isNullOrBlank(value)) {
            throw new IllegalArgumentException("The config value must not be null or blank.");
        }

        this.value = value;
        this.stringValue = String.valueOf(value);
        this.fromSource = fromSource;
    }


    @NotNull
    public T getValue() {
        return value;
    }

    @NotNull
    public String getStringValue() {
        return stringValue;
    }

    public ConfigurationSource getFromSource() {
        return fromSource;
    }

    public String getFromSourceTypeName() {
        return fromSource != null ? fromSource.getTypeName() : null;
    }

    public Date getTime() {
        return time;
    }

    public String getStringTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    }
}
