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
package io.seata.server.console.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static io.seata.spring.boot.autoconfigure.StarterConstants.SEATA_PREFIX;

/**
 * Configuration console properties
 *
 * @author: Yuzhiqiang
 */
@Component
@ConfigurationProperties(SEATA_PREFIX + ".console.configuration")
public class ConfigurationConsoleProperties {

    /**
     * The configuration items displayed in the console
     */
    private List<ConfigInfo> configItems = new ArrayList<>();


    public List<ConfigInfo> getConfigItems() {
        return configItems;
    }

    public void setConfigItems(List<ConfigInfo> configItems) {
        this.configItems = configItems;
    }
}
