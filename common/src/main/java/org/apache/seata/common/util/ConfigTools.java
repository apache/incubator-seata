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

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Configuration utility tools for encryption and decryption
 */
public class ConfigTools {

    /**
     * Generate key pair
     *
     * @return the key pair
     * @throws Exception when error occurs
     */
    public static KeyPair getKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return keyPair;
    }

    /**
     * Obtain the public key (Base64 encoding)
     *
     * @param keyPair the key pair
     * @return the public key string
     */
    public static String getPublicKey(KeyPair keyPair) {
        PublicKey publicKey = keyPair.getPublic();
        byte[] bytes = publicKey.getEncoded();
        return byte2Base64(bytes);
    }

    /**
     * Obtain the private key (Base64 encoding)
     *
     * @param keyPair the key pair
     * @return the private key string
     */
    public static String getPrivateKey(KeyPair keyPair) {
        PrivateKey privateKey = keyPair.getPrivate();
        byte[] bytes = privateKey.getEncoded();
        return byte2Base64(bytes);
    }

    /**
     * Convert Base64 encoded public key to PublicKey object
     *
     * @param pubStr the public key string
     * @return the public key
     * @throws Exception when error occurs
     */
    public static PublicKey string2PublicKey(String pubStr) throws Exception {
        byte[] keyBytes = base642Byte(pubStr);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        return publicKey;
    }

    /**
     * Convert Base64 encoded private key to PrivateKey object
     *
     * @param priStr the private key string
     * @return the private key
     * @throws Exception when error occurs
     */
    public static PrivateKey string2PrivateKey(String priStr) throws Exception {
        byte[] keyBytes = base642Byte(priStr);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        return privateKey;
    }

    /**
     * Public key encryption
     *
     * @param content the content to encrypt
     * @param pubStr the public key string
     * @return the encrypted content
     * @throws Exception when error occurs
     */
    public static String publicEncrypt(String content, String pubStr) throws Exception {
        PublicKey publicKey = string2PublicKey(pubStr);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] bytes = cipher.doFinal(content.getBytes());
        return byte2Base64(bytes);
    }

    /**
     * Public key decryption
     *
     * @param content the content to decrypt
     * @param pubStr the public key string
     * @return the decrypted content
     * @throws Exception when error occurs
     */
    public static String publicDecrypt(String content, String pubStr) throws Exception {
        PublicKey publicKey = string2PublicKey(pubStr);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        byte[] bytes = cipher.doFinal(base642Byte(content));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Private key encryption
     *
     * @param content the content to encrypt
     * @param priStr the private key string
     * @return the encrypted content
     * @throws Exception when error occurs
     */
    public static String privateEncrypt(String content, String priStr) throws Exception {
        PrivateKey privateKey = string2PrivateKey(priStr);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] bytes = cipher.doFinal(content.getBytes());
        return byte2Base64(bytes);
    }

    /**
     * Private key decryption
     *
     * @param content the content to decrypt
     * @param priStr the private key string
     * @return the decrypted content
     * @throws Exception when error occurs
     */
    public static String privateDecrypt(String content, String priStr) throws Exception {
        PrivateKey privateKey = string2PrivateKey(priStr);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] bytes = cipher.doFinal(base642Byte(content));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Convert byte array to Base64 encoding
     *
     * @param bytes the byte array
     * @return the Base64 encoded string
     */
    public static String byte2Base64(byte[] bytes) {
        return new String(Base64.getEncoder().encode(bytes), StandardCharsets.UTF_8);
    }

    /**
     * Convert Base64 encoding to byte array
     *
     * @param base64Key the Base64 encoded string
     * @return the byte array
     */
    public static byte[] base642Byte(String base64Key) {
        return Base64.getDecoder().decode(base64Key);
    }
}
