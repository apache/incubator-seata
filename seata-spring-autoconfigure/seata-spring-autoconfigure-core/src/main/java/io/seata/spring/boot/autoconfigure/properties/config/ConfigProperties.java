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

import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = CONFIG_PREFIX)
public class ConfigProperties {
    /**
     * file, nacos, apollo, zk, consul, etcd3, springCloudConfig
     */
    private String type = "file";
    /**
     * properties,yaml(only type in nacos, zk, consul, etcd3)
     */
    private String dataType ;

    public String getType() {
        return type;
    }

    public ConfigProperties setType(String type) {
        this.type = type;
        return this;
    }

    public String getDataType() {
        return dataType;
    }

    public ConfigProperties setDataType(String dataType) {
        this.dataType = dataType;
        return this;
    }
}
