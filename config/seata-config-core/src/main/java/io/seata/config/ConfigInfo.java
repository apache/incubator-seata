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

import java.util.Objects;
import javax.annotation.Nonnull;

import io.seata.config.source.ConfigSource;

/**
 * The type ConfigInfo.
 *
 * @author wang.liang
 */
public class ConfigInfo {

    private final String dataId;
    private final String value;
    private final ConfigSource source;


    public ConfigInfo(String dataId, String value, ConfigSource source) {
        Objects.requireNonNull(value, "The 'config' value must not be null.");
        Objects.requireNonNull(source, "The 'source' must not be null.");

        this.dataId = dataId;
        this.value = value;
        this.source = source;
    }

    public String getDataId() {
        return dataId;
    }

    @Nonnull
    public String getValue() {
        return value;
    }

    @Nonnull
    public ConfigSource getSource() {
        return source;
    }

    public String getSourceName() {
        return source.getName();
    }


    @Override
    public String toString() {
        return '{' +
                "v=" + value +
                ", s=" + getSourceName() +
                '}';
    }
}
