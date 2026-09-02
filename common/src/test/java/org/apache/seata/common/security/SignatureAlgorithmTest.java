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
package org.apache.seata.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SignatureAlgorithmTest {

    @Test
    void fromWireName_maps_to_expected_enum() {
        assertEquals(SignatureAlgorithm.HMAC_SHA256, SignatureAlgorithm.fromWireName("HMAC-SHA256"));
        assertEquals(SignatureAlgorithm.HMAC_SHA512, SignatureAlgorithm.fromWireName("HMAC-SHA512"));
    }

    @Test
    void fromWireName_rejects_null_or_blank() {
        assertThrows(IllegalArgumentException.class, () -> SignatureAlgorithm.fromWireName(null));
        assertThrows(IllegalArgumentException.class, () -> SignatureAlgorithm.fromWireName(""));
    }

    @Test
    void fromWireName_rejects_unknown_algorithm() {
        assertThrows(IllegalArgumentException.class, () -> SignatureAlgorithm.fromWireName("MD5"));
    }

    @Test
    void jca_names_are_stable() {
        assertEquals("HmacSHA256", SignatureAlgorithm.HMAC_SHA256.jcaName());
        assertEquals("HmacSHA512", SignatureAlgorithm.HMAC_SHA512.jcaName());
    }
}
