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
package io.seata.config;

/**
 * The type Configuration change event.
 */
@Deprecated
public class ConfigurationChangeEvent {
    private String dataId;
    private String oldValue;
    private String newValue;
    private String namespace;
    private ConfigurationChangeType changeType;
    private static final String DEFAULT_NAMESPACE = "DEFAULT";

    /**
     * Instantiates a new Configuration change event.
     */
    public ConfigurationChangeEvent() {

    }

    /**
     * Instantiates a new Configuration change event.
     *
     * @param dataId   the data id
     * @param newValue the new value
     */
    public ConfigurationChangeEvent(String dataId, String newValue) {
        this(dataId, DEFAULT_NAMESPACE, null, newValue, ConfigurationChangeType.MODIFY);
    }

    /**
     * Instantiates a new Configuration change event.
     *
     * @param dataId    the data id
     * @param namespace the namespace
     * @param oldValue  the old value
     * @param newValue  the new value
     * @param type      the type
     */
    public ConfigurationChangeEvent(String dataId, String namespace, String oldValue, String newValue,
                                    ConfigurationChangeType type) {
        this.dataId = dataId;
        this.namespace = namespace;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changeType = type;
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
     * @return the data id
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
     * @return the old value
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
     * @return the new value
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
     * @return the change type
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
     * @return the namespace
     */
    public ConfigurationChangeEvent setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }
}
