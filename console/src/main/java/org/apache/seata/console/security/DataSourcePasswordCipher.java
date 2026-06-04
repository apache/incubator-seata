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
package org.apache.seata.console.security;

import org.apache.seata.common.util.ConfigTools;
import org.apache.seata.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyPair;

@Component
public class DataSourcePasswordCipher {

    private final String publicKey;

    private final String privateKey;

    public DataSourcePasswordCipher(
            @Value("${seata.businessDataSources.encryption.public-key:}") String configuredPublicKey,
            @Value("${seata.businessDataSources.encryption.private-key:}") String configuredPrivateKey)
            throws Exception {
        if (StringUtils.isNotBlank(configuredPublicKey) && StringUtils.isNotBlank(configuredPrivateKey)) {
            this.publicKey = configuredPublicKey;
            this.privateKey = configuredPrivateKey;
            return;
        }
        KeyPair keyPair = ConfigTools.getKeyPair();
        this.publicKey = ConfigTools.getPublicKey(keyPair);
        this.privateKey = ConfigTools.getPrivateKey(keyPair);
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String decrypt(String encryptedPassword) {
        if (StringUtils.isBlank(encryptedPassword)) {
            return "";
        }
        try {
            return ConfigTools.privateDecrypt(encryptedPassword, privateKey);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to decrypt datasource password");
        }
    }
}
