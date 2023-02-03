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
package io.seata.config.changelistener;

import java.util.Objects;
import javax.annotation.Nonnull;

import io.seata.config.source.ConfigSource;

/**
 * The type Configuration change event.
 *
 * @author slievrly
 */
public class ConfigurationChangeEvent {

    private String dataId;
    private String oldValue;
    private String newValue;
    private String namespace;
    private ConfigurationChangeType changeType;

    @Nonnull
    private final ConfigSource changeEventSource;


    public ConfigurationChangeEvent(@Nonnull ConfigSource changeEventSource) {
        Objects.requireNonNull(changeEventSource, "The 'changeEventSource' must not be null.");
        this.changeEventSource = changeEventSource;
    }

    public ConfigurationChangeEvent(String dataId, String namespace, String oldValue, String newValue,
                                    ConfigurationChangeType type, @Nonnull ConfigSource changeEventSource) {
        Objects.requireNonNull(changeEventSource, "The 'changeEventSource' must not be null.");

        this.dataId = dataId;
        this.namespace = namespace;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changeType = type;
        this.changeEventSource = changeEventSource;
    }


    /**
     * Gets data id.
     *
     * @return the data id
     */
    public String getDataId() {
        return dataId;
    }

    /**
     * Sets data id.
     *
     * @param dataId the data id
     */
    public ConfigurationChangeEvent setDataId(String dataId) {
        this.dataId = dataId;
        return this;
    }

    /**
     * Gets old value.
     *
     * @return the old value
     */
    public String getOldValue() {
        return oldValue;
    }

    /**
     * Sets old value.
     *
     * @param oldValue the old value
     */
    public ConfigurationChangeEvent setOldValue(String oldValue) {
        this.oldValue = oldValue;
        return this;
    }

    /**
     * Gets new value.
     *
     * @return the new value
     */
    public String getNewValue() {
        return newValue;
    }

    /**
     * Sets new value.
     *
     * @param newValue the new value
     */
    public ConfigurationChangeEvent setNewValue(String newValue) {
        this.newValue = newValue;
        return this;
    }

    /**
     * Gets change type.
     *
     * @return the change type
     */
    public ConfigurationChangeType getChangeType() {
        return changeType;
    }

    /**
     * Sets change type.
     *
     * @param changeType the change type
     */
    public ConfigurationChangeEvent setChangeType(ConfigurationChangeType changeType) {
        this.changeType = changeType;
        return this;
    }

    /**
     * Gets namespace.
     *
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets namespace.
     *
     * @param namespace the namespace
     */
    public ConfigurationChangeEvent setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    /**
     * Gets change event source
     *
     * @return the change event source
     */
    @Nonnull
    public ConfigSource getChangeEventSource() {
        return changeEventSource;
    }

    /**
     * Gets type name of change event source
     *
     * @return the type name
     */
    @Nonnull
    public String getChangeEventSourceTypeName() {
        return getChangeEventSource().getName();
    }


    @Override
    public String toString() {
        return "[" +
                "dataId='" + dataId + '\'' +
                ", oldValue='" + oldValue + '\'' +
                ", newValue='" + newValue + '\'' +
                ", namespace='" + namespace + '\'' +
                ", changeType=" + changeType +
                ", changeEventSource=" + changeEventSource.getName() +
                ']';
    }
}
