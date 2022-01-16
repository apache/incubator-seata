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
package io.seata.spring.boot.autoconfigure.properties.server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.common.DefaultValues.DEFAULT_SERVER_CERTIFICATE_TYPE;
import static io.seata.common.DefaultValues.DEFAULT_SERVER_ENABLE_TLS;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SERVER_TLS_PREFIX;

@Component
@ConfigurationProperties(prefix = SERVER_TLS_PREFIX)
public class ServerTlsProperties {
    private String certificateType = DEFAULT_SERVER_CERTIFICATE_TYPE;
    private String certificatePath;
    private String certificatePassword;
    private boolean enableTls = DEFAULT_SERVER_ENABLE_TLS;
    private String keyFilePath;
    private String tlsVersion = null;

    public String getCertificateType() {
        return this.certificateType;
    }

    public ServerTlsProperties setCertificateType(String certificateType) {
        this.certificateType = certificateType;
        return this;
    }

    public String getCertificatePath() { return this.certificatePath; }

    public ServerTlsProperties setCertificatePath(String certificatePath) {
        this.certificatePath = certificatePath;
        return this;
    }

    public String getCertificatePassword() { return this.certificatePassword; }

    public ServerTlsProperties setCertificatePassword(String certificatePassword) {
        this.certificatePassword = certificatePassword;
        return this;
    }

    public boolean isEnableTls() {
        return enableTls;
    }

    public ServerTlsProperties setEnableTls(boolean enableTls) {
        this.enableTls = enableTls;
        return this;
    }

    public String getKeyFilePath() { return this.keyFilePath; }

    public ServerTlsProperties setKeyFilePath(String keyFilePath) {
        this.keyFilePath = keyFilePath;
        return this;
    }

    public String getTlsVersion() {
        return tlsVersion;
    }

    public ServerTlsProperties setTlsVersion(String tlsVersion) {
        this.tlsVersion = tlsVersion;
        return this;
    }
}
