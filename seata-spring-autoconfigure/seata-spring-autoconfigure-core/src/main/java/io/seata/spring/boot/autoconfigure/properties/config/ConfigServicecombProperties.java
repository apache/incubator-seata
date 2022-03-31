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

package io.seata.spring.boot.autoconfigure.properties.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_SERVICECOMB_PREFIX;

/**
 * @author zhaozhongwei22@163.com
 */
@Component
@ConfigurationProperties(prefix = CONFIG_SERVICECOMB_PREFIX)
public class ConfigServicecombProperties {
    private String address;
    private String type;
    private String customLabel;
    private String customLabelValue;
    private String enableCustomConfig;
    private String enableServiceConfig;
    private String enableAppConfig;
    private String firstPullRequired;

    public String getCustomLabel() {
        return customLabel;
    }

    public ConfigServicecombProperties setCustomLabel(String customLabel) {
        this.customLabel = customLabel;
        return this;
    }

    public String getCustomLabelValue() {
        return customLabelValue;
    }

    public ConfigServicecombProperties setCustomLabelValue(String customLabelValue) {
        this.customLabelValue = customLabelValue;
        return this;
    }

    public String getEnableCustomConfig() {
        return enableCustomConfig;
    }

    public ConfigServicecombProperties setEnableCustomConfig(String enableCustomConfig) {
        this.enableCustomConfig = enableCustomConfig;
        return this;
    }

    public String getEnableServiceConfig() {
        return enableServiceConfig;
    }

    public ConfigServicecombProperties setEnableServiceConfig(String enableServiceConfig) {
        this.enableServiceConfig = enableServiceConfig;
        return this;
    }

    public String getEnableAppConfig() {
        return enableAppConfig;
    }

    public ConfigServicecombProperties setEnableAppConfig(String enableAppConfig) {
        this.enableAppConfig = enableAppConfig;
        return this;
    }

    public String getFirstPullRequired() {
        return firstPullRequired;
    }

    public ConfigServicecombProperties setFirstPullRequired(String firstPullRequired) {
        this.firstPullRequired = firstPullRequired;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public ConfigServicecombProperties setAddress(String address) {
        this.address = address;
        return this;
    }

    public String getType() {
        return type;
    }

    public ConfigServicecombProperties setType(String type) {
        this.type = type;
        return this;
    }
}
