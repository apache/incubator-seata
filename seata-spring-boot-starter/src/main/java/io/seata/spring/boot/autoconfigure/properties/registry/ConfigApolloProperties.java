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
package io.seata.spring.boot.autoconfigure.properties.registry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_APOLLO_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = CONFIG_APOLLO_PREFIX)
public class ConfigApolloProperties {
    private String appId = "seata-server";
    private String apolloMeta = "http://192.168.1.204:8801";

    public String getAppId() {
        return appId;
    }

    public ConfigApolloProperties setAppId(String appId) {
        this.appId = appId;
        return this;
    }

    public String getApolloMeta() {
        return apolloMeta;
    }

    public ConfigApolloProperties setApolloMeta(String apolloMeta) {
        this.apolloMeta = apolloMeta;
        return this;
    }
}
