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
package io.seata.common.util;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;
import javax.crypto.Cipher;

/**
 * @author funkye
 */
public class ConfigTools {

    // generate key pair
    public static KeyPair getKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return keyPair;
    }

    // obtain the public key (Base64 encoding)
    public static String getPublicKey(KeyPair keyPair) {
        PublicKey publicKey = keyPair.getPublic();
        byte[] bytes = publicKey.getEncoded();
        return byte2Base64(bytes);
    }

    // obtain the private key (Base64 encoding)
    public static String getPrivateKey(KeyPair keyPair) {
        PrivateKey privateKey = keyPair.getPrivate();
        byte[] bytes = privateKey.getEncoded();
        return byte2Base64(bytes);
    }

    // convert Base64 encoded public key to PublicKey object
    public static PublicKey string2PublicKey(String pubStr) throws Exception {
        byte[] keyBytes = base642Byte(pubStr);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        return publicKey;
    }

    // convert Base64 encoded private key to PrivateKey object
    public static PrivateKey string2PrivateKey(String priStr) throws Exception {
        byte[] keyBytes = base642Byte(priStr);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        return privateKey;
    }

    // public key encryption
    public static String publicEncrypt(String content, String pubStr) throws Exception {
        PublicKey publicKey = string2PublicKey(pubStr);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] bytes = cipher.doFinal(content.getBytes());
        return byte2Base64(bytes);
    }

    // public key decryption
    public static String publicDecrypt(String content, String pubStr) throws Exception {
        PublicKey publicKey = string2PublicKey(pubStr);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        byte[] bytes = cipher.doFinal(base642Byte(content));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // private key encryption
    public static String privateEncrypt(String content, String priStr) throws Exception {
        PrivateKey privateKey = string2PrivateKey(priStr);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] bytes = cipher.doFinal(content.getBytes());
        return byte2Base64(bytes);
    }

    // private key decryption
    public static String privateDecrypt(String content, String priStr) throws Exception {
        PrivateKey privateKey = string2PrivateKey(priStr);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] bytes = cipher.doFinal(base642Byte(content));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // byte array to Base64 encoding
    public static String byte2Base64(byte[] bytes) {
        return new String(Base64.getEncoder().encode(bytes), StandardCharsets.UTF_8);
    }

    // Base64 encoding to byte array
    public static byte[] base642Byte(String base64Key) {
        return Base64.getDecoder().decode(base64Key);
    }

    public static void main(String[] args) throws Exception {
        Scanner scan = new Scanner(System.in);
        KeyPair keyPair = getKeyPair();
        String publicKeyStr = ConfigTools.getPublicKey(keyPair);
        String privateKeyStr = ConfigTools.getPrivateKey(keyPair);
        System.out.println("publicKeyStr:\n" + publicKeyStr);
        System.out.println("privateKeyStr:\n" + privateKeyStr);
        System.out.println(
            "after the key is generated, please keep your key pair properly, if you need to encrypt, please enter your database password");
        System.out.println("input 'q' exit");
        while (scan.hasNextLine()) {
            String password = scan.nextLine();
            if (StringUtils.isNotBlank(password) && !"q".equalsIgnoreCase(password)) {
                String byte2Base64 = ConfigTools.privateEncrypt(password, privateKeyStr);
                System.out.println("encryption completed: \n" + byte2Base64);
            }
            break;
        }
        scan.close();
    }

}
