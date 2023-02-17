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
package io.seata.core.auth;

import io.seata.common.util.ConfigTools;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * adapt ram sign interface
 *
 * @author onlinechild
 */
public class RamSignAdapter {
    
    private static final String SHA256_ENCRYPT = "HmacSHA256";
    
    private static final String PREFIX = "aliyun_v4";
    
    private static final String CONSTANT = "aliyun_v4_request";
    
    private static final String DEFAULT_REGION = "cn-beijing";
    
    private static final String DEFAULT_PRODCUT_CODE = "seata";
    
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    /**
     * get date level signing key
     *
     * @param secret     secret
     * @param date       data, yyyyMMdd
     * @param signMethod HmacSHA256
     * @return date level signing key
     */
    private static byte[] getDateSigningKey(String secret, String date, String signMethod) {
        try {
            Mac mac = Mac.getInstance(signMethod);
            mac.init(new SecretKeySpec((PREFIX + secret).getBytes(StandardCharsets.UTF_8), signMethod));
            return mac.doFinal(date.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("unsupport Algorithm:" + signMethod);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("InvalidKey");
        }
    }
    
    /**
     * get date&region level signing key
     *
     * @param secret     secret
     * @param date       data
     * @param region     region
     * @param signMethod HmacSHA256
     * @return date&region level signing key
     */
    private static byte[] getRegionSigningKey(String secret, String date, String region, String signMethod) {
        byte[] dateSignkey = getDateSigningKey(secret, date, signMethod);
        try {
            Mac mac = Mac.getInstance(signMethod);
            mac.init(new SecretKeySpec(dateSignkey, signMethod));
            return mac.doFinal(region.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("unsupport Algorithm:" + signMethod);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("InvalidKey");
        }
    }
    
    /**
     * get date&region&product level signing key
     *
     * @param secret      secret
     * @param date        date
     * @param region      region
     * @param productCode productCode
     * @param signMethod  signMethod
     * @return date&region&product level signing key
     */
    private static byte[] getProductSigningKey(String secret, String date, String region, String productCode, String signMethod) {
        byte[] regionSignkey = getRegionSigningKey(secret, date, region, signMethod);
        try {
            Mac mac = Mac.getInstance(signMethod);
            mac.init(new SecretKeySpec(regionSignkey, signMethod));
            byte[] thirdSigningKey = mac.doFinal(productCode.getBytes(StandardCharsets.UTF_8));
            mac = Mac.getInstance(signMethod);
            mac.init(new SecretKeySpec(thirdSigningKey, signMethod));
            return mac.doFinal(CONSTANT.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("unsupport Algorithm:" + signMethod);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("InvalidKey");
        }
    }
    
    /**
     * get ram sign Sign with hmac SHA1 encrtpt
     *
     * @param encryptText encrypt text
     * @param encryptKey  encrypt key
     * @return base64 string
     */
    public static String getRamSign(String encryptText, String encryptKey) {
        try {
            String[] encryptData = encryptText.split(",");
            byte[] data = getProductSigningKey(encryptKey,
                    LocalDateTime.ofEpochSecond(Long.parseLong(encryptData[2]) / 1000, 0, ZoneOffset.UTC).format(DTF),
                    DEFAULT_REGION, DEFAULT_PRODCUT_CODE, SHA256_ENCRYPT);
            // Construct a key according to the given byte array, and the second parameter specifies the name of a key algorithm
            SecretKey secretKey = new SecretKeySpec(data, SHA256_ENCRYPT);
            // Generate a Mac object specifying Mac algorithm
            Mac mac = Mac.getInstance(SHA256_ENCRYPT);
            // Initialize the Mac object with the given key
            mac.init(secretKey);
            byte[] text = encryptText.getBytes(StandardCharsets.UTF_8);
            byte[] textFinal = mac.doFinal(text);
            // Complete Mac operation, base64 encoding, convert byte array to string
            return ConfigTools.byte2Base64(textFinal);
        } catch (Exception e) {
            throw new RuntimeException("get ram sign with hmacSHA1Encrypt fail", e);
        }
    }
    
}
