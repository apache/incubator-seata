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

/**
 * The type Configuration change event.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2019 /11/13
 */
public class ConfigurationChangeEvent {

    private String dataId;
    private String oldValue;
    private String newValue;
    private String namespace;
    private ConfigurationChangeType changeType;

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
     */
    public void setDataId(String dataId) {
        this.dataId = dataId;
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
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
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
    public void setNewValue(String newValue) {
        this.newValue = newValue;
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
    public void setChangeType(ConfigurationChangeType changeType) {
        this.changeType = changeType;
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
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
