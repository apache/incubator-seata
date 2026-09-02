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

import java.util.Objects;

/**
 * Maps an HTTP request path to the {@link Permission} needed to serve it, then verifies the
 * caller identity holds that permission <em>and</em> that the requested namespace/cluster/vgroup
 * fall within the identity's allow-lists.
 *
 * <p>Route → permission mapping is centralised here so operators can audit it at a glance
 * without walking the controller layer.
 */
public final class PermissionChecker {

    /**
     * Decide whether the caller may perform the request.
     *
     * @param identity  authenticated caller (never null — this runs after signature verification)
     * @param method    HTTP method, upper-case
     * @param path      request path (no query string, no host)
     * @param namespace target namespace as parsed from body/query (may be null for read-only endpoints)
     * @param cluster   target cluster (may be null)
     * @param vGroup    target vgroup (may be null)
     * @return {@code true} iff the request is authorized
     */
    public boolean check(
            ClusterIdentity identity, String method, String path, String namespace, String cluster, String vGroup) {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(path, "path");

        Permission required = requiredPermission(method, path);
        if (required == null) {
            // No route mapping = deny by default. Fail-closed is the correct posture here.
            return false;
        }
        if (!identity.hasPermission(required)) {
            return false;
        }
        if (namespace != null && !identity.isNamespaceAllowed(namespace)) {
            return false;
        }
        if (cluster != null && !identity.isClusterAllowed(cluster)) {
            return false;
        }
        if (vGroup != null && !identity.isVgroupAllowed(vGroup)) {
            return false;
        }
        return true;
    }

    /**
     * @return the permission required to serve the given route, or {@code null} if the route
     *         is not recognised (which the caller should treat as "forbidden")
     */
    public Permission requiredPermission(String method, String path) {
        if ("POST".equals(method) && path.endsWith("/register")) {
            return Permission.REGISTER;
        }
        if ("POST".equals(method) && path.endsWith("/batchRegister")) {
            return Permission.REGISTER;
        }
        if ("POST".equals(method) && path.endsWith("/unregister")) {
            return Permission.REGISTER;
        }
        if ("POST".equals(method) && (path.endsWith("/addGroup") || path.endsWith("/changeGroup"))) {
            return Permission.VGROUP_WRITE;
        }
        if ("GET".equals(method)
                && (path.endsWith("/discovery")
                        || path.endsWith("/clusters")
                        || path.endsWith("/clusterData")
                        || path.endsWith("/namespace"))) {
            return Permission.CONSOLE_READ;
        }
        if ("POST".equals(method) && path.endsWith("/watch")) {
            // Watch is called by RM/TM at scale. Treat as a lightweight read.
            return Permission.CONSOLE_READ;
        }
        if (path.contains("/console/")) {
            return "GET".equals(method) ? Permission.CONSOLE_READ : Permission.CONSOLE_WRITE;
        }
        return null;
    }
}
