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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;

/**
 * Core HMAC sign / verify logic. Deliberately kept small and dependency-free so it can be
 * shared between {@code common}, {@code namingserver}, {@code seata-server} and clients.
 *
 * <h3>Signing</h3>
 * {@link #sign(CanonicalRequest, byte[])} produces the Base64-encoded MAC that goes into the
 * {@link SecurityConstants#HEADER_SIGNATURE} header.
 *
 * <h3>Verifying</h3>
 * {@link #verify(CanonicalRequest, byte[], String)} recomputes the MAC and compares it with the
 * received value using a <b>constant-time</b> comparison. That defends against timing side
 * channels that would otherwise let an attacker guess the signature one byte at a time.
 *
 * <p>The class is stateless and thread-safe: each call creates a fresh {@link Mac} instance.
 * {@code Mac} itself is <em>not</em> thread-safe if reused across threads, but constructing one is
 * cheap compared to the network round trip we are protecting.
 */
public final class HmacSigner {

    /** Reject secrets shorter than this to prevent trivial brute force. 256 bit minimum. */
    public static final int MIN_KEY_LENGTH_BYTES = 32;

    private HmacSigner() {
        // utility
    }

    /**
     * Sign a canonical request.
     *
     * @param request the request being signed
     * @param secret  raw shared secret bytes (≥ {@link #MIN_KEY_LENGTH_BYTES})
     * @return Base64-encoded MAC
     * @throws IllegalArgumentException if the secret is too short
     * @throws IllegalStateException    if the JCA provider is missing the algorithm
     */
    public static String sign(CanonicalRequest request, byte[] secret) {
        Objects.requireNonNull(request, "request");
        validateSecret(secret);
        byte[] mac = mac(
                request.getAlgorithm(),
                secret,
                SignatureCanonicalizer.canonicalize(request).getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(mac);
    }

    /**
     * Verify a signature. Returns {@code true} iff the recomputed signature exactly matches the
     * received value, in constant time relative to the signature length.
     *
     * @param request           canonical request as the receiver reconstructed it
     * @param secret            the shared secret for the caller identity
     * @param receivedSignature Base64 string from {@link SecurityConstants#HEADER_SIGNATURE}
     * @return {@code true} iff the signature is valid
     */
    public static boolean verify(CanonicalRequest request, byte[] secret, String receivedSignature) {
        if (receivedSignature == null || receivedSignature.isEmpty()) {
            return false;
        }
        byte[] received;
        try {
            received = Base64.getDecoder().decode(receivedSignature);
        } catch (IllegalArgumentException badBase64) {
            // Not valid Base64 → cannot possibly match. Do NOT surface the reason
            // to the caller: it would leak information about the failure mode.
            return false;
        }
        byte[] expected = mac(
                request.getAlgorithm(),
                secret,
                SignatureCanonicalizer.canonicalize(request).getBytes(StandardCharsets.UTF_8));
        return constantTimeEquals(expected, received);
    }

    /**
     * Length-safe constant-time byte array comparison. Any length or content difference
     * fails; iteration never short-circuits on the first differing byte, so timing does
     * not leak which byte first differs.
     *
     * <p>The loop is bounded by the shorter input because in this class the reference
     * value {@code expected} always has a fixed algorithm-defined MAC length (32 bytes
     * for HMAC-SHA-256). Only {@code received} is attacker-controlled, and its length is
     * a value the attacker already picked, so timing tied to {@code received.length}
     * cannot reveal any secret material.
     */
    static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return false;
        }
        // XOR the length difference into the accumulator too: mismatched lengths must
        // never fast-fail (that would leak length via timing).
        int diff = a.length ^ b.length;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            diff |= (a[i] ^ b[i]);
        }
        return diff == 0;
    }

    static byte[] mac(SignatureAlgorithm algorithm, byte[] secret, byte[] message) {
        try {
            Mac mac = Mac.getInstance(algorithm.jcaName());
            mac.init(new SecretKeySpec(secret, algorithm.jcaName()));
            return mac.doFinal(message);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MAC algorithm " + algorithm.jcaName() + " not available on this JVM", e);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("invalid HMAC key", e);
        }
    }

    private static void validateSecret(byte[] secret) {
        if (secret == null || secret.length < MIN_KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException("shared secret must be at least "
                    + MIN_KEY_LENGTH_BYTES + " bytes (256 bit); got "
                    + (secret == null ? 0 : secret.length));
        }
    }
}
