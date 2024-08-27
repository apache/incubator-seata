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
package org.apache.seata.spring.boot.autoconfigure.properties;

import org.apache.seata.common.DefaultValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SEATA_PREFIX;


@Component
@ConfigurationProperties(prefix = SEATA_PREFIX)
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
     * data source proxy mode
     */
    private String dataSourceProxyMode = DefaultValues.DEFAULT_DATA_SOURCE_PROXY_MODE;
    /**
     * Whether use JDK proxy instead of CGLIB proxy
     */
    private boolean useJdkProxy = false;
    /**
     * Whether to expose the proxy object through AopContext.
     * Setting this to true allows AopContext.currentProxy() to be used to obtain the current proxy,
     * which can be useful for invoking methods annotated with @GlobalTransactional within the same class.
     */
    private boolean exposeProxy = false;
    /**
     * The scan packages. If empty, will scan all beans.
     */
    private String[] scanPackages = {};
    /**
     * Specifies beans that won't be scanned in the GlobalTransactionScanner
     */
    private String[] excludesForScanning = {};
    /**
     * Specifies which datasource bean are not eligible for auto-proxying
     */
    private String[] excludesForAutoProxying = {};

    /**
     * used for aliyun accessKey
     */
    private String accessKey;

    /**
     * used for aliyun secretKey
     */
    private String secretKey;

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
        if (applicationId == null) {
            applicationId = springCloudAlibabaConfiguration.getApplicationId();
        }
        return applicationId;
    }

    public SeataProperties setApplicationId(String applicationId) {
        this.applicationId = applicationId;
        return this;
    }

    public String getTxServiceGroup() {
        if (txServiceGroup == null) {
            txServiceGroup = DefaultValues.DEFAULT_TX_GROUP;
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

    public String getDataSourceProxyMode() {
        return dataSourceProxyMode;
    }

    public void setDataSourceProxyMode(String dataSourceProxyMode) {
        this.dataSourceProxyMode = dataSourceProxyMode;
    }

    public boolean isUseJdkProxy() {
        return useJdkProxy;
    }

    public SeataProperties setUseJdkProxy(boolean useJdkProxy) {
        this.useJdkProxy = useJdkProxy;
        return this;
    }

    public boolean isExposeProxy() {
        return exposeProxy;
    }

    public void setExposeProxy(boolean exposeProxy) {
        this.exposeProxy = exposeProxy;
    }

    public String[] getExcludesForAutoProxying() {
        return excludesForAutoProxying;
    }

    public SeataProperties setExcludesForAutoProxying(String[] excludesForAutoProxying) {
        this.excludesForAutoProxying = excludesForAutoProxying;
        return this;
    }

    public String[] getScanPackages() {
        return scanPackages;
    }

    public SeataProperties setScanPackages(String[] scanPackages) {
        this.scanPackages = scanPackages;
        return this;
    }

    public String[] getExcludesForScanning() {
        return excludesForScanning;
    }

    public SeataProperties setExcludesForScanning(String[] excludesForScanning) {
        this.excludesForScanning = excludesForScanning;
        return this;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public SeataProperties setAccessKey(String accessKey) {
        this.accessKey = accessKey;
        return this;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public SeataProperties setSecretKey(String secretKey) {
        this.secretKey = secretKey;
        return this;
    }
}
