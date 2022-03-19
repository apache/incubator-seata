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

/**
 * adapt ram sign interface
 *
 * @author onlinechild
 */
public class RamSignAdapter {
    
    public static final String SHA_ENCRYPT = "HmacSHA1";
    
    /**
     * get ram sign
     * Sign with hmac SHA1 encrtpt
     *
     * @param encryptText encrypt text
     * @param encryptKey  encrypt key
     * @return base64 string
     */
    public static String getRamSign(String encryptText, String encryptKey) {
        try {
            byte[] data = encryptKey.getBytes(StandardCharsets.UTF_8);
            // Construct a key according to the given byte array, and the second parameter specifies the name of a key algorithm
            SecretKey secretKey = new SecretKeySpec(data, SHA_ENCRYPT);
            // Generate a Mac object specifying Mac algorithm
            Mac mac = Mac.getInstance(SHA_ENCRYPT);
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
