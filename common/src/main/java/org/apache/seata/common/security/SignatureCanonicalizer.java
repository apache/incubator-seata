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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Produces the deterministic {@code signString} that both signer and verifier feed into HMAC.
 *
 * <p>Format:
 * <pre>
 * signString = METHOD          + "\n"
 *            + PATH            + "\n"
 *            + CANONICAL_QUERY + "\n"    // key1=v1&amp;key2=v2 sorted by key, URL-encoded
 *            + CLUSTER_ID      + "\n"
 *            + TIMESTAMP       + "\n"
 *            + NONCE           + "\n"
 *            + SHA256_HEX(body)
 * </pre>
 *
 * <p>Rationale for each field:
 * <ul>
 *   <li><b>method</b>: prevents an attacker from replaying a signed GET as a DELETE.</li>
 *   <li><b>path</b>: prevents routing a signed {@code /addVGroup} to {@code /removeVGroup}.</li>
 *   <li><b>canonical query</b>: prevents parameter tampering (e.g. changing {@code vGroup}).
 *       Keys are sorted to make the encoding deterministic across languages.</li>
 *   <li><b>clusterId + timestamp + nonce</b>: bind the signature to a specific caller
 *       and freshness window; the verifier extracts these from headers and reuses them here.</li>
 *   <li><b>body digest</b>: signing the body directly would force the verifier to buffer
 *       the entire payload; a fixed-size SHA-256 digest keeps the {@code signString} bounded.</li>
 * </ul>
 */
public final class SignatureCanonicalizer {

    private SignatureCanonicalizer() {
        // utility
    }

    /**
     * Build the canonical string for the given request.
     *
     * @param request the request being signed (or verified)
     * @return the string to feed into the MAC
     */
    public static String canonicalize(CanonicalRequest request) {
        StringBuilder sb = new StringBuilder(256);
        sb.append(request.getMethod()).append('\n');
        sb.append(request.getPath()).append('\n');
        sb.append(canonicalQuery(request.getQueryParams())).append('\n');
        sb.append(request.getClusterId()).append('\n');
        sb.append(request.getTimestampMillis()).append('\n');
        sb.append(request.getNonce()).append('\n');
        sb.append(sha256Hex(request.getBody()));
        return sb.toString();
    }

    /**
     * Sort {@code query} by key ascending and produce a URL-encoded {@code k=v&k=v} string.
     * Empty map → empty string. Null values are treated as empty strings (avoids differing
     * encoders producing {@code k=null} vs {@code k=}).
     */
    static String canonicalQuery(Map<String, String> query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        List<String> keys = new ArrayList<>(query.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = query.get(key);
            if (i > 0) {
                sb.append('&');
            }
            sb.append(urlEncode(key)).append('=').append(urlEncode(value == null ? "" : value));
        }
        return sb.toString();
    }

    /**
     * Lowercase hex-encoded SHA-256 digest of the given bytes. Empty input still produces a
     * well-defined digest ({@code e3b0c44298fc...}), which is what we want for empty-body requests.
     */
    static String sha256Hex(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(body == null ? new byte[0] : body);
            return toHexLower(out);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by every JCA provider; this is effectively unreachable.
            throw new IllegalStateException("SHA-256 not available in this JVM", e);
        }
    }

    /** URL-encode using RFC 3986 semantics; wraps checked exception. */
    private static String urlEncode(String value) {
        try {
            // URLEncoder produces application/x-www-form-urlencoded ('+' for space);
            // convert to '%20' to match RFC 3986 which most servers expect on query strings.
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 unavailable — impossible on a conforming JVM", e);
        }
    }

    /** Lowercase hex encoding without external deps. */
    private static String toHexLower(byte[] bytes) {
        char[] hexArray = "0123456789abcdef".toCharArray();
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = hexArray[v >>> 4];
            out[i * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(out);
    }
}
