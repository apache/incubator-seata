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
package org.apache.seata.console.utils;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenUtilsTest {

    @Test
    void rejectsMissingOrBlankSecret() {
        for (String secret : new String[] {null, "", " \t\n"}) {
            assertThrows(IllegalArgumentException.class, () -> tokens(secret).validateSecretKey());
        }
    }

    @Test
    void rejectsInvalidBase64() {
        assertThrows(
                IllegalArgumentException.class, () -> tokens("!".repeat(48)).validateSecretKey());
    }

    @Test
    void rejectsSecretBelowDecodedLengthBoundary() {
        String secret = randomSecret(31);
        assertThrows(IllegalArgumentException.class, () -> tokens(secret).validateSecretKey());
    }

    @Test
    void rejectsPublishedDefaultAndEquivalentEncoding() {
        String publishedDefault = "SeataSecretKey0c382ef121d778043159209298fd40bf3850a017";
        String equivalentEncoding =
                Base64.getEncoder().encodeToString(Base64.getDecoder().decode(publishedDefault));
        for (String secret : new String[] {publishedDefault, equivalentEncoding}) {
            assertThrows(IllegalArgumentException.class, () -> tokens(secret).validateSecretKey());
        }
    }

    @Test
    void acceptsRandomSecretsAtAndAboveDecodedLengthBoundary() {
        for (int length : new int[] {32, 64}) {
            String secret = randomSecret(length);
            assertDoesNotThrow(() -> tokens(secret).validateSecretKey());
        }
    }

    private JwtTokenUtils tokens(String secret) {
        JwtTokenUtils tokens = new JwtTokenUtils();
        ReflectionTestUtils.setField(tokens, "secretKey", secret);
        return tokens;
    }

    private String randomSecret(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
