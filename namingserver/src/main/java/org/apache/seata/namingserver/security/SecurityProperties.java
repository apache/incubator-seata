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
package org.apache.seata.namingserver.security;

import java.util.ArrayList;
import java.util.List;

import org.apache.seata.common.security.SecurityConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration mapped from {@code seata.security.*} in {@code application.yml}.
 *
 * <p>Design decisions:
 * <ul>
 *   <li><b>Secret indirection</b>: each cluster carries a {@code secret-ref} string (e.g.
 *       {@code env:MY_VAR}, {@code vault:secret/foo}) instead of an inline value. The
 *       {@code SecretResolver} resolves it at boot time. This keeps plaintext keys out of
 *       config files and out of {@code /actuator/configprops} dumps.</li>
 *   <li><b>Mode enum</b> ({@code DISABLED / WARN / ENFORCE}) gives operators a safe two-step
 *       rollout: deploy in {@code WARN} to observe, then flip to {@code ENFORCE}.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "seata.security")
public class SecurityProperties {

    /** Whether the security layer is loaded at all. When false, no filter is registered. */
    private boolean enabled = false;

    /** {@link Mode#ENFORCE} rejects unsigned or invalid requests. {@link Mode#WARN} only logs. */
    private Mode mode = Mode.ENFORCE;

    /** Clock-skew tolerance for {@code X-Seata-Timestamp}, in seconds. */
    private long replayWindowSeconds = SecurityConstants.DEFAULT_REPLAY_WINDOW_SECONDS;

    /** How long a nonce lives in the anti-replay cache, in minutes. */
    private long nonceCacheMinutes = SecurityConstants.DEFAULT_NONCE_CACHE_MINUTES;

    /** URL paths (Ant-style) that bypass the filter entirely, e.g. the health endpoint. */
    private List<String> excludePaths = new ArrayList<>();

    /** Identities the NamingServer will trust on the inbound path. */
    private List<ClusterConfig> clusters = new ArrayList<>();

    /** Identity the NamingServer uses when reaching out to TC (self-identification). */
    private OutboundConfig outbound = new OutboundConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public long getReplayWindowSeconds() {
        return replayWindowSeconds;
    }

    public void setReplayWindowSeconds(long replayWindowSeconds) {
        this.replayWindowSeconds = replayWindowSeconds;
    }

    public long getNonceCacheMinutes() {
        return nonceCacheMinutes;
    }

    public void setNonceCacheMinutes(long nonceCacheMinutes) {
        this.nonceCacheMinutes = nonceCacheMinutes;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public List<ClusterConfig> getClusters() {
        return clusters;
    }

    public void setClusters(List<ClusterConfig> clusters) {
        this.clusters = clusters;
    }

    public OutboundConfig getOutbound() {
        return outbound;
    }

    public void setOutbound(OutboundConfig outbound) {
        this.outbound = outbound;
    }

    public enum Mode {
        /** Skip all auth logic. */
        DISABLED,
        /** Log unauthorized requests but do not reject them. */
        WARN,
        /** Reject unauthorized requests with 401/403. */
        ENFORCE
    }

    /** Per-caller config binding. See {@link ClusterIdentity} for the runtime shape. */
    public static class ClusterConfig {
        private String id;
        private String secretRef;
        private List<String> allowedNamespaces = new ArrayList<>();
        private List<String> allowedClusters = new ArrayList<>();
        private List<String> allowedVgroups = new ArrayList<>();
        private List<Permission> permissions = new ArrayList<>();

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getSecretRef() { return secretRef; }
        public void setSecretRef(String secretRef) { this.secretRef = secretRef; }
        public List<String> getAllowedNamespaces() { return allowedNamespaces; }
        public void setAllowedNamespaces(List<String> allowedNamespaces) { this.allowedNamespaces = allowedNamespaces; }
        public List<String> getAllowedClusters() { return allowedClusters; }
        public void setAllowedClusters(List<String> allowedClusters) { this.allowedClusters = allowedClusters; }
        public List<String> getAllowedVgroups() { return allowedVgroups; }
        public void setAllowedVgroups(List<String> allowedVgroups) { this.allowedVgroups = allowedVgroups; }
        public List<Permission> getPermissions() { return permissions; }
        public void setPermissions(List<Permission> permissions) { this.permissions = permissions; }
    }

    public static class OutboundConfig {
        private String clusterId;
        private String secretRef;

        public String getClusterId() { return clusterId; }
        public void setClusterId(String clusterId) { this.clusterId = clusterId; }
        public String getSecretRef() { return secretRef; }
        public void setSecretRef(String secretRef) { this.secretRef = secretRef; }
    }
}
