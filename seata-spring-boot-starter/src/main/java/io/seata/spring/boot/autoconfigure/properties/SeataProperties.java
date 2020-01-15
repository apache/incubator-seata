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
package io.seata.spring.boot.autoconfigure.properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import static io.seata.spring.boot.autoconfigure.StarterConstants.SEATA_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@ConfigurationProperties(prefix = SEATA_PREFIX)
@EnableConfigurationProperties(SpringCloudAlibabaConfiguration.class)
public class SeataProperties {
    /**
     * whether enable auto configuration
     */
    private boolean enabled = true;
    /**
     * application id
     */
    private String applicationId;
    /**
     * transaction service group
     */
    private String txServiceGroup;
    /**
     * Whether enable auto proxying of datasource bean
     */
    private boolean enableAutoDataSourceProxy = true;
    /**
     * Whether use JDK proxy instead of CGLIB proxy
     */
    private boolean useJdkProxy = false;

    @Autowired
    private SpringCloudAlibabaConfiguration springCloudAlibabaConfiguration;

    public boolean isEnabled() {
        return enabled;
    }

    public SeataProperties setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getApplicationId() {
        if (null == applicationId) {
            applicationId = springCloudAlibabaConfiguration.getApplicationId();
        }
        return applicationId;
    }

    public SeataProperties setApplicationId(String applicationId) {
        this.applicationId = applicationId;
        return this;
    }

    public String getTxServiceGroup() {
        if (null == txServiceGroup) {
            txServiceGroup = springCloudAlibabaConfiguration.getTxServiceGroup();
        }
        return txServiceGroup;
    }

    public SeataProperties setTxServiceGroup(String txServiceGroup) {
        this.txServiceGroup = txServiceGroup;
        return this;
    }

    public boolean isEnableAutoDataSourceProxy() {
        return enableAutoDataSourceProxy;
    }

    public SeataProperties setEnableAutoDataSourceProxy(boolean enableAutoDataSourceProxy) {
        this.enableAutoDataSourceProxy = enableAutoDataSourceProxy;
        return this;
    }

    public boolean isUseJdkProxy() {
        return useJdkProxy;
    }

    public SeataProperties setUseJdkProxy(boolean useJdkProxy) {
        this.useJdkProxy = useJdkProxy;
        return this;
    }
}
