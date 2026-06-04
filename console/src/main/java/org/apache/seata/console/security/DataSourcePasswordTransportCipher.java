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

import org.apache.seata.common.util.StringUtils;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

@Component
public class DataSourcePasswordTransportCipher {

    private static final String RSA_ALGORITHM = "RSA";

    private static final String RSA_OAEP_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    private static final int RSA_KEY_SIZE = 2048;

    private static final String TRANSPORT_CIPHER_PREFIX = "rsa:";

    private final KeyPair keyPair;

    public DataSourcePasswordTransportCipher() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            generator.initialize(RSA_KEY_SIZE);
            this.keyPair = generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to initialize datasource password transport cipher");
        }
    }

    public String getPublicKey() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    public boolean isEncrypted(String password) {
        return StringUtils.isNotBlank(password) && password.startsWith(TRANSPORT_CIPHER_PREFIX);
    }

    public String encrypt(String password) {
        if (StringUtils.isBlank(password)) {
            return "";
        }
        try {
            Cipher cipher = newCipher(Cipher.ENCRYPT_MODE);
            byte[] ciphertext = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
            return TRANSPORT_CIPHER_PREFIX + Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to encrypt datasource password for transport");
        }
    }

    public String decrypt(String encryptedPassword) {
        if (StringUtils.isBlank(encryptedPassword)) {
            return "";
        }
        if (!isEncrypted(encryptedPassword)) {
            throw new IllegalArgumentException("Invalid datasource password transport ciphertext");
        }
        try {
            String ciphertext = encryptedPassword.substring(TRANSPORT_CIPHER_PREFIX.length());
            byte[] payload = Base64.getDecoder().decode(ciphertext);
            Cipher cipher = newCipher(Cipher.DECRYPT_MODE);
            return new String(cipher.doFinal(payload), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to decrypt datasource password for transport");
        }
    }

    private Cipher newCipher(int mode) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_OAEP_TRANSFORMATION);
        OAEPParameterSpec spec =
                new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
        cipher.init(mode, mode == Cipher.ENCRYPT_MODE ? keyPair.getPublic() : keyPair.getPrivate(), spec);
        return cipher;
    }
}
