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

import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_SERVICECOMB_ACCOUNT_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_SERVICECOMB_HEALTHCHECK_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_SERVICECOMB_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_SERVICECOMB_PULL_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_SERVICECOMB_SSL_PREFIX;

/**
 * @author zhaozhongwei22@163.com
 */
@Component
@ConfigurationProperties(prefix = REGISTRY_SERVICECOMB_PREFIX)
public class RegistryServicecombProperties {
    private String address;
    private String project;
    private String serviceName;
    private String appName;
    private String initialStatus;
    private String enableLongPolling;
    private String pollingWaitInSeconds;
    private String environment;
    private String enableversionAppConfig;
    private String version;
    private String allowCrossApp;

    public String getAllowCrossApp() {
        return allowCrossApp;
    }

    public RegistryServicecombProperties setAllowCrossApp(String allowCrossApp) {
        this.allowCrossApp = allowCrossApp;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public RegistryServicecombProperties setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getInitialStatus() {
        return initialStatus;
    }

    public RegistryServicecombProperties setInitialStatus(String initialStatus) {
        this.initialStatus = initialStatus;
        return this;
    }

    public String getEnableLongPolling() {
        return enableLongPolling;
    }

    public RegistryServicecombProperties setEnableLongPolling(String enableLongPolling) {
        this.enableLongPolling = enableLongPolling;
        return this;
    }

    public String getPollingWaitInSeconds() {
        return pollingWaitInSeconds;
    }

    public RegistryServicecombProperties setPollingWaitInSeconds(String pollingWaitInSeconds) {
        this.pollingWaitInSeconds = pollingWaitInSeconds;
        return this;
    }

    public String getEnvironment() {
        return environment;
    }

    public RegistryServicecombProperties setEnvironment(String environment) {
        this.environment = environment;
        return this;
    }

    public String getEnableversionAppConfig() {
        return enableversionAppConfig;
    }

    public RegistryServicecombProperties setEnableversionAppConfig(String enableversionAppConfig) {
        this.enableversionAppConfig = enableversionAppConfig;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public RegistryServicecombProperties setAddress(String address) {
        this.address = address;
        return this;
    }

    public String getProject() {
        return project;
    }

    public RegistryServicecombProperties setProject(String project) {
        this.project = project;
        return this;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    @Component
    @ConfigurationProperties(prefix = REGISTRY_SERVICECOMB_HEALTHCHECK_PREFIX)
    public static class HealthCheck {
        private int interval;
        private int times;

        public int getInterval() {
            return interval;
        }

        public HealthCheck setInterval(int interval) {
            this.interval = interval;
            return this;
        }

        public int getTimes() {
            return times;
        }

        public HealthCheck setTimes(int times) {
            this.times = times;
            return this;
        }
    }

    @Component
    @ConfigurationProperties(prefix = REGISTRY_SERVICECOMB_PULL_PREFIX)
    public static class Pull {
        private int interval;

        public int getInterval() {
            return interval;
        }

        public Pull setInterval(int interval) {
            this.interval = interval;
            return this;
        }
    }

    @Component
    @ConfigurationProperties(prefix = REGISTRY_SERVICECOMB_ACCOUNT_PREFIX)
    public static class Account {
        private String name;
        private String password;

        public String getName() {
            return name;
        }

        public Account setName(String name) {
            this.name = name;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public Account setPassword(String password) {
            this.password = password;
            return this;
        }
    }

    @Component
    @ConfigurationProperties(prefix = REGISTRY_SERVICECOMB_SSL_PREFIX)
    public static class Ssl {
        private String enabled;
        private String ciphers;
        private String authPeer;
        private String checkCNHost;
        private String checkCNWhite;
        private String checkCNWhiteFile;
        private String allowRenegotiate;
        private String storePath;
        private String trustStore;
        private String trustStoreType;
        private String trustStoreValue;
        private String keyStore;
        private String keyStoreType;
        private String keyStoreValue;
        private String crl;
        private String sslCustomClass;

        public String getEnabled() {
            return enabled;
        }

        public Ssl setEnabled(String enabled) {
            this.enabled = enabled;
            return this;
        }

        public String getSslCustomClass() {
            return sslCustomClass;
        }

        public Ssl setSslCustomClass(String sslCustomClass) {
            this.sslCustomClass = sslCustomClass;
            return this;
        }

        public String getCiphers() {
            return ciphers;
        }

        public Ssl setCiphers(String ciphers) {
            this.ciphers = ciphers;
            return this;
        }

        public String getAuthPeer() {
            return authPeer;
        }

        public Ssl setAuthPeer(String authPeer) {
            this.authPeer = authPeer;
            return this;
        }

        public String getCheckCNHost() {
            return checkCNHost;
        }

        public Ssl setCheckCNHost(String checkCNHost) {
            this.checkCNHost = checkCNHost;
            return this;
        }

        public String getCheckCNWhite() {
            return checkCNWhite;
        }

        public Ssl setCheckCNWhite(String checkCNWhite) {
            this.checkCNWhite = checkCNWhite;
            return this;
        }

        public String getCheckCNWhiteFile() {
            return checkCNWhiteFile;
        }

        public Ssl setCheckCNWhiteFile(String checkCNWhiteFile) {
            this.checkCNWhiteFile = checkCNWhiteFile;
            return this;
        }

        public String getAllowRenegotiate() {
            return allowRenegotiate;
        }

        public Ssl setAllowRenegotiate(String allowRenegotiate) {
            this.allowRenegotiate = allowRenegotiate;
            return this;
        }

        public String getStorePath() {
            return storePath;
        }

        public Ssl setStorePath(String storePath) {
            this.storePath = storePath;
            return this;
        }

        public String getTrustStore() {
            return trustStore;
        }

        public Ssl setTrustStore(String trustStore) {
            this.trustStore = trustStore;
            return this;
        }

        public String getTrustStoreType() {
            return trustStoreType;
        }

        public Ssl setTrustStoreType(String trustStoreType) {
            this.trustStoreType = trustStoreType;
            return this;
        }

        public String getTrustStoreValue() {
            return trustStoreValue;
        }

        public Ssl setTrustStoreValue(String trustStoreValue) {
            this.trustStoreValue = trustStoreValue;
            return this;
        }

        public String getKeyStore() {
            return keyStore;
        }

        public Ssl setKeyStore(String keyStore) {
            this.keyStore = keyStore;
            return this;
        }

        public String getKeyStoreType() {
            return keyStoreType;
        }

        public Ssl setKeyStoreType(String keyStoreType) {
            this.keyStoreType = keyStoreType;
            return this;
        }

        public String getKeyStoreValue() {
            return keyStoreValue;
        }

        public Ssl setKeyStoreValue(String keyStoreValue) {
            this.keyStoreValue = keyStoreValue;
            return this;
        }

        public String getCrl() {
            return crl;
        }

        public Ssl setCrl(String crl) {
            this.crl = crl;
            return this;
        }
    }
}
