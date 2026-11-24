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
package org.apache.seata.server.security;

import org.apache.seata.common.security.SecurityConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for TC's inbound auth pipeline. Bound to
 * {@code seata.registry.seata.security.inbound.*} to keep the namespace close to the
 * existing {@code seata.registry.seata.*} settings that TC already uses.
 *
 * <p>Outbound (TC → NamingServer) auth continues to be handled by the existing
 * {@code NamingserverRegistryServiceImpl} — this class only wires the inbound side.
 */
@ConfigurationProperties(prefix = "seata.registry.seata.security.inbound")
public class ServerSecurityProperties {

    private boolean enabled = false;

    private Mode mode = Mode.ENFORCE;

    private long replayWindowSeconds = SecurityConstants.DEFAULT_REPLAY_WINDOW_SECONDS;

    private long nonceCacheMinutes = SecurityConstants.DEFAULT_NONCE_CACHE_MINUTES;

    /** Paths the filter must not touch (e.g. actuator health). Ant-style, supports trailing {@code /**}. */
    private List<String> excludePaths = new ArrayList<>();

    /** Accepted NamingServer identities. Multiple entries let operators rotate secrets safely. */
    private List<CallerConfig> allowedCallers = new ArrayList<>();

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

    public void setReplayWindowSeconds(long s) {
        this.replayWindowSeconds = s;
    }

    public long getNonceCacheMinutes() {
        return nonceCacheMinutes;
    }

    public void setNonceCacheMinutes(long m) {
        this.nonceCacheMinutes = m;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> p) {
        this.excludePaths = p;
    }

    public List<CallerConfig> getAllowedCallers() {
        return allowedCallers;
    }

    public void setAllowedCallers(List<CallerConfig> c) {
        this.allowedCallers = c;
    }

    public enum Mode {
        /** Auth pipeline runs but only logs violations. */
        WARN,
        /** Auth pipeline runs and rejects violations with 401/403. */
        ENFORCE
    }

    public static class CallerConfig {
        private String id;
        private String secretRef;
        private List<CallerPermission> permissions = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSecretRef() {
            return secretRef;
        }

        public void setSecretRef(String secretRef) {
            this.secretRef = secretRef;
        }

        public List<CallerPermission> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<CallerPermission> p) {
            this.permissions = p;
        }
    }
}
