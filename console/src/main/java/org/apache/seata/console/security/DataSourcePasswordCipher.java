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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class DataSourcePasswordCipher {

    private static final String AES_ALGORITHM = "AES";

    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int GCM_IV_LENGTH = 12;

    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec secretKeySpec;

    private final boolean enabled;

    private final SecureRandom secureRandom = new SecureRandom();

    public DataSourcePasswordCipher(
            @Value("${seata.security.secretKey}") String secretKey,
            @Value("${seata.businessDataSources.encryption.enabled:true}") boolean enabled) {
        if (StringUtils.isBlank(secretKey)) {
            throw new IllegalArgumentException("seata.security.secretKey cannot be empty");
        }
        this.secretKeySpec = new SecretKeySpec(sha256(secretKey), AES_ALGORITHM);
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String decrypt(String encryptedPassword) {
        if (StringUtils.isBlank(encryptedPassword)) {
            return "";
        }
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedPassword);
            if (payload.length <= GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid datasource password ciphertext");
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(payload, GCM_IV_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to decrypt datasource password");
        }
    }

    public String encrypt(String password) {
        if (StringUtils.isBlank(password)) {
            return "";
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to encrypt datasource password");
        }
    }

    private byte[] sha256(String secretKey) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(secretKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to initialize datasource password cipher");
        }
    }
}
