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
 * Full verification pipeline for an inbound signed request. Combines four checks in this order:
 *
 * <ol>
 *   <li><b>Freshness</b> — timestamp must be within {@code now ± replayWindow}. Rejects
 *       captured-but-stale requests.</li>
 *   <li><b>Signature validity</b> — the HMAC must match, computed in constant time.</li>
 *   <li><b>Nonce uniqueness</b> — the {@code (clusterId, nonce)} pair must not have been
 *       seen in the recent past. Rejects verbatim replays inside the freshness window.</li>
 *   <li>(Caller-supplied) <b>Authorization</b> — mapping cluster-id to allowed namespaces /
 *       clusters / vgroups happens outside this class in {@code PermissionChecker}, since it
 *       depends on receiver-specific data models.</li>
 * </ol>
 *
 * <p>Authenticate before storing the nonce so forged requests cannot fill the replay cache
 * or consume a legitimate caller's nonce. Admission remains atomic for concurrent replays.
 */
public final class SignatureVerifier {

    private final NonceCache nonceCache;

    private final long replayWindowMillis;

    private final NonceCache.Clock clock;

    /**
     * @param nonceCache          the replay-detection cache
     * @param replayWindowMillis  maximum accepted clock skew, in milliseconds
     * @param clock               time source (defaults to {@link System#currentTimeMillis()} if null)
     */
    public SignatureVerifier(NonceCache nonceCache, long replayWindowMillis, NonceCache.Clock clock) {
        this.nonceCache = Objects.requireNonNull(nonceCache, "nonceCache");
        if (replayWindowMillis <= 0) {
            throw new IllegalArgumentException("replayWindowMillis must be > 0");
        }
        this.replayWindowMillis = replayWindowMillis;
        this.clock = clock == null ? System::currentTimeMillis : clock;
    }

    /**
     * Verify one canonical request against a shared secret.
     *
     * @param request           the reconstructed canonical request
     * @param secret            the shared secret associated with {@code request.getClusterId()}
     * @param receivedSignature Base64 signature from the {@code X-Seata-Sign} header
     * @return a structured verdict — never {@code null}
     */
    public VerificationResult verify(CanonicalRequest request, byte[] secret, String receivedSignature) {
        Objects.requireNonNull(request, "request");
        if (secret == null || secret.length == 0) {
            return VerificationResult.failure(
                    SecurityConstants.ErrorCode.UNKNOWN_CLUSTER_ID,
                    "no secret configured for cluster-id: " + request.getClusterId());
        }
        if (receivedSignature == null || receivedSignature.isEmpty()) {
            return VerificationResult.failure(
                    SecurityConstants.ErrorCode.MISSING_SIGNATURE,
                    "missing " + SecurityConstants.HEADER_SIGNATURE + " header");
        }

        // 1) Freshness — cheap, must come before any keyed hashing.
        long now = clock.now();
        long delta = Math.abs(now - request.getTimestampMillis());
        if (delta > replayWindowMillis) {
            return VerificationResult.failure(
                    SecurityConstants.ErrorCode.TIMESTAMP_SKEW,
                    "timestamp skew of " + delta + "ms exceeds window " + replayWindowMillis + "ms");
        }

        // 2) Authenticate before retaining attacker-controlled nonces.
        boolean valid;
        try {
            valid = HmacSigner.verify(request, secret, receivedSignature);
        } catch (IllegalArgumentException bad) {
            return VerificationResult.failure(SecurityConstants.ErrorCode.BAD_REQUEST, bad.getMessage());
        }
        if (!valid) {
            return VerificationResult.failure(SecurityConstants.ErrorCode.BAD_SIGNATURE, "signature does not match");
        }

        boolean fresh;
        try {
            fresh = nonceCache.putIfAbsent(request.getClusterId(), request.getNonce());
        } catch (IllegalArgumentException bad) {
            return VerificationResult.failure(SecurityConstants.ErrorCode.BAD_REQUEST, bad.getMessage());
        }
        if (!fresh) {
            return VerificationResult.failure(
                    SecurityConstants.ErrorCode.REPLAY_DETECTED,
                    "nonce already used for cluster-id " + request.getClusterId());
        }

        return VerificationResult.ok();
    }
}
