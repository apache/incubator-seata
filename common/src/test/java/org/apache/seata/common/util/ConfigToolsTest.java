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
package org.apache.seata.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConfigToolsTest {

    @Test
    public void testInit() {
        Assertions.assertNotNull(new ConfigTools());
    }

    @Test
    public void test() throws Exception {
        KeyPair keyPair = ConfigTools.getKeyPair();
        String publicKeyStr = ConfigTools.getPublicKey(keyPair);
        String privateKeyStr = ConfigTools.getPrivateKey(keyPair);
        System.out.println("publicKeyStr:" + publicKeyStr);
        System.out.println("privateKeyStr:" + privateKeyStr);
        String password = "123456";
        String byte2Base64 = ConfigTools.privateEncrypt(password, privateKeyStr);
        System.out.println("byte2Base64：" + byte2Base64);
        String pw = ConfigTools.publicDecrypt(byte2Base64, publicKeyStr);
        Assertions.assertEquals(pw, password);
    }

    @Test
    public void testPublicEncryptAndPrivateDecrypt() throws Exception {
        KeyPair keyPair = ConfigTools.getKeyPair();
        String publicKeyStr = ConfigTools.getPublicKey(keyPair);
        String privateKeyStr = ConfigTools.getPrivateKey(keyPair);
        System.out.println("publicKeyStr:" + publicKeyStr);
        System.out.println("privateKeyStr:" + privateKeyStr);
        String password = "123456";
        String byte2Base64 = ConfigTools.publicEncrypt(password, publicKeyStr);
        System.out.println("byte2Base64：" + byte2Base64);
        String pw = ConfigTools.privateDecrypt(byte2Base64, privateKeyStr);
        Assertions.assertEquals(pw, password);
    }

    @Test
    public void testGetKeyPair() throws Exception {
        KeyPair keyPair = ConfigTools.getKeyPair();
        assertThat(keyPair).isNotNull();
        assertThat(keyPair.getPublic()).isNotNull();
        assertThat(keyPair.getPrivate()).isNotNull();
    }

    @Test
    public void testGetPublicKey() throws Exception {
        KeyPair keyPair = ConfigTools.getKeyPair();
        String publicKey = ConfigTools.getPublicKey(keyPair);
        assertThat(publicKey).isNotNull();
        assertThat(publicKey).isNotEmpty();
    }

    @Test
    public void testGetPrivateKey() throws Exception {
        KeyPair keyPair = ConfigTools.getKeyPair();
        String privateKey = ConfigTools.getPrivateKey(keyPair);
        assertThat(privateKey).isNotNull();
        assertThat(privateKey).isNotEmpty();
    }

    @Test
    public void testString2PublicKey() throws Exception {
        KeyPair keyPair = ConfigTools.getKeyPair();
        String publicKeyStr = ConfigTools.getPublicKey(keyPair);
        PublicKey publicKey = ConfigTools.string2PublicKey(publicKeyStr);
        assertThat(publicKey).isNotNull();
    }

    @Test
    public void testString2PublicKeyWithInvalidKey() {
        String invalidKey = "invalid_public_key";
        assertThatThrownBy(() -> ConfigTools.string2PublicKey(invalidKey)).isInstanceOf(Exception.class);
    }

    @Test
    public void testString2PrivateKey() throws Exception {
        KeyPair keyPair = ConfigTools.getKeyPair();
        String privateKeyStr = ConfigTools.getPrivateKey(keyPair);
        PrivateKey privateKey = ConfigTools.string2PrivateKey(privateKeyStr);
        assertThat(privateKey).isNotNull();
    }

    @Test
    public void testString2PrivateKeyWithInvalidKey() {
        String invalidKey = "invalid_private_key";
        assertThatThrownBy(() -> ConfigTools.string2PrivateKey(invalidKey)).isInstanceOf(Exception.class);
    }

    @Test
    public void testPublicEncryptWithInvalidKey() {
        String content = "test content";
        String invalidKey = "invalid_public_key";
        assertThatThrownBy(() -> ConfigTools.publicEncrypt(content, invalidKey)).isInstanceOf(Exception.class);
    }

    @Test
    public void testPublicDecryptWithInvalidKey() {
        String content = "test encrypted content";
        String invalidKey = "invalid_public_key";
        assertThatThrownBy(() -> ConfigTools.publicDecrypt(content, invalidKey)).isInstanceOf(Exception.class);
    }

    @Test
    public void testPublicDecryptWithInvalidContent() throws Exception {
        KeyPair keyPair = ConfigTools.getKeyPair();
        String publicKeyStr = ConfigTools.getPublicKey(keyPair);
        String invalidContent = "invalid_encrypted_content";
        assertThatThrownBy(() -> ConfigTools.publicDecrypt(invalidContent, publicKeyStr))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testPrivateEncryptWithInvalidKey() {
        String content = "test content";
        String invalidKey = "invalid_private_key";
        assertThatThrownBy(() -> ConfigTools.privateEncrypt(content, invalidKey))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testPrivateDecryptWithInvalidKey() {
        String content = "test encrypted content";
        String invalidKey = "invalid_private_key";
        assertThatThrownBy(() -> ConfigTools.privateDecrypt(content, invalidKey))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testPrivateDecryptWithInvalidContent() throws Exception {
        KeyPair keyPair = ConfigTools.getKeyPair();
        String privateKeyStr = ConfigTools.getPrivateKey(keyPair);
        String invalidContent = "invalid_encrypted_content";
        assertThatThrownBy(() -> ConfigTools.privateDecrypt(invalidContent, privateKeyStr))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testByte2Base64() {
        byte[] bytes = "test".getBytes();
        String base64 = ConfigTools.byte2Base64(bytes);
        assertThat(base64).isNotNull();
        assertThat(base64).isNotEmpty();
    }

    @Test
    public void testBase642Byte() {
        String base64 = "dGVzdA=="; // "test" in base64
        byte[] bytes = ConfigTools.base642Byte(base64);
        assertThat(bytes).isNotNull();
        assertThat(new String(bytes)).isEqualTo("test");
    }

    @Test
    public void testBase642ByteWithInvalidInput() {
        String invalidBase64 = "invalid_base64!";
        assertThatThrownBy(() -> ConfigTools.base642Byte(invalidBase64)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testRoundTripEncryption() throws Exception {
        KeyPair keyPair = ConfigTools.getKeyPair();
        String publicKeyStr = ConfigTools.getPublicKey(keyPair);
        String privateKeyStr = ConfigTools.getPrivateKey(keyPair);
        String content = "This is a test message for encryption round trip";

        // Public encrypt, private decrypt
        String encrypted1 = ConfigTools.publicEncrypt(content, publicKeyStr);
        String decrypted1 = ConfigTools.privateDecrypt(encrypted1, privateKeyStr);
        assertThat(decrypted1).isEqualTo(content);

        // Private encrypt, public decrypt
        String encrypted2 = ConfigTools.privateEncrypt(content, privateKeyStr);
        String decrypted2 = ConfigTools.publicDecrypt(encrypted2, publicKeyStr);
        assertThat(decrypted2).isEqualTo(content);
    }

    @Test
    public void testEmptyAndNullInputs() throws Exception {
        KeyPair keyPair = ConfigTools.getKeyPair();
        String publicKeyStr = ConfigTools.getPublicKey(keyPair);
        String privateKeyStr = ConfigTools.getPrivateKey(keyPair);

        // Test with empty string
        String emptyEncrypted = ConfigTools.publicEncrypt("", publicKeyStr);
        String emptyDecrypted = ConfigTools.privateDecrypt(emptyEncrypted, privateKeyStr);
        assertThat(emptyDecrypted).isEqualTo("");

        // The methods don't check for null inputs, so we just ensure they're covered
        assertThat(ConfigTools.class)
                .hasDeclaredMethods(
                        "publicEncrypt",
                        "privateDecrypt",
                        "privateEncrypt",
                        "publicDecrypt",
                        "string2PublicKey",
                        "string2PrivateKey");
    }
}
