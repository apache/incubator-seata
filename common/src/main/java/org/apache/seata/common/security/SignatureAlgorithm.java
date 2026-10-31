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

import java.util.Objects;

/**
 * Supported MAC / signature algorithms. Kept as an enum with a wire identifier so we can
 * (a) advertise the algorithm in {@link SecurityConstants#HEADER_SIGN_ALG} for future upgrades,
 * and (b) refuse unknown values instead of silently defaulting.
 */
public enum SignatureAlgorithm {

    /** HMAC-SHA256 — 256 bit output; used as the default. */
    HMAC_SHA256("HMAC-SHA256", "HmacSHA256"),

    /** HMAC-SHA512 — 512 bit output; opt-in for higher security workloads. */
    HMAC_SHA512("HMAC-SHA512", "HmacSHA512");

    private final String wireName;

    private final String jcaName;

    SignatureAlgorithm(String wireName, String jcaName) {
        this.wireName = wireName;
        this.jcaName = jcaName;
    }

    /** The identifier that appears on the wire (in the {@code X-Seata-Sign-Alg} header). */
    public String wireName() {
        return wireName;
    }

    /** JCA {@code Mac.getInstance(...)} algorithm name. */
    public String jcaName() {
        return jcaName;
    }

    /**
     * Parse a wire name back to an enum value.
     *
     * @param wireName value from the {@code X-Seata-Sign-Alg} header, e.g. {@code HMAC-SHA256}
     * @return the matching enum
     * @throws IllegalArgumentException if the value is null, blank, or unknown
     */
    public static SignatureAlgorithm fromWireName(String wireName) {
        if (wireName == null || wireName.isEmpty()) {
            throw new IllegalArgumentException("signature algorithm must not be blank");
        }
        for (SignatureAlgorithm alg : values()) {
            if (Objects.equals(alg.wireName, wireName)) {
                return alg;
            }
        }
        throw new IllegalArgumentException("unsupported signature algorithm: " + wireName);
    }
}
