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

/**
 * Header names and error codes used by the HMAC-based authentication protocol
 * between Seata NamingServer and Seata Server (TC), and by Client (TM/RM) when
 * upgraded to HMAC signing.
 *
 * <p>Header prefix {@code X-Seata-} keeps these fields distinguishable from
 * platform-standard HTTP headers and easy to allow through reverse proxies.
 */
public final class SecurityConstants {

    /** Identifies the caller cluster/tenant. Receiver uses this to look up the shared secret. */
    public static final String HEADER_CLUSTER_ID = "X-Seata-Cluster-Id";

    /** Millisecond timestamp of when the request was signed. Used to bound the replay window. */
    public static final String HEADER_TIMESTAMP = "X-Seata-Timestamp";

    /** Random nonce (typically a UUID). Combined with timestamp to prevent replay. */
    public static final String HEADER_NONCE = "X-Seata-Nonce";

    /** Signature algorithm identifier, e.g. {@code HMAC-SHA256}. Reserved for future upgrades. */
    public static final String HEADER_SIGN_ALG = "X-Seata-Sign-Alg";

    /** Base64-encoded signature bytes. */
    public static final String HEADER_SIGNATURE = "X-Seata-Sign";

    /**
     * Default maximum clock skew allowed between caller and receiver, in seconds.
     * Requests older or newer than {@code now ± this} are rejected.
     */
    public static final long DEFAULT_REPLAY_WINDOW_SECONDS = 300L;

    /**
     * Default nonce cache TTL, in minutes. Must be strictly greater than
     * {@link #DEFAULT_REPLAY_WINDOW_SECONDS} to guarantee that any timestamp
     * inside the window still finds its nonce in the cache.
     */
    public static final long DEFAULT_NONCE_CACHE_MINUTES = 10L;

    private SecurityConstants() {
        // constants holder
    }

    /**
     * Machine-readable authentication error codes. Emitted via response header
     * {@code X-Seata-Auth-Error} and logged for audit / metrics.
     */
    public enum ErrorCode {
        /** No signature headers present. Only allowed when {@code security.mode=warn}. */
        MISSING_SIGNATURE,
        /** {@link SecurityConstants#HEADER_CLUSTER_ID} value has no matching identity on the receiver. */
        UNKNOWN_CLUSTER_ID,
        /** Timestamp is outside {@code now ± replayWindow}. */
        TIMESTAMP_SKEW,
        /** Nonce already seen inside the replay window. */
        REPLAY_DETECTED,
        /** Recomputed signature does not match the provided one. */
        BAD_SIGNATURE,
        /** Signature algorithm value is unknown / unsupported. */
        UNSUPPORTED_ALG,
        /** Caller identity is authentic but lacks the required permission for the resource. */
        FORBIDDEN,
        /** Malformed request: missing required field, bad Base64, etc. */
        BAD_REQUEST
    }
}
