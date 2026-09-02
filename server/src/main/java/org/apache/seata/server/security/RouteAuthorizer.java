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

/**
 * Maps TC's inbound HTTP routes to a required {@link CallerPermission}. Any path that
 * isn't matched here returns {@code null}, which the filter treats as "not part of the
 * NamingServer control-plane surface" and passes through untouched.
 *
 * <p>Kept intentionally small — only paths that could conceivably be reached by
 * NamingServer or by an operator's console proxy need protection here. Netty ports and
 * anything auto-served by Spring Boot infrastructure (actuator, static files) are
 * expected to be either excluded up-front or protected by other means.
 */
public final class RouteAuthorizer {

    /** @return required permission or {@code null} if the route is not recognised */
    public CallerPermission requiredPermission(String method, String path) {
        if (path == null || method == null) {
            return null;
        }
        // vGroup mapping write is the highest-privilege pathway from NamingServer to TC.
        if ("GET".equals(method)
                && (path.endsWith("/vgroup/v1/addVGroup") || path.endsWith("/vgroup/v1/removeVGroup"))) {
            return CallerPermission.VGROUP_WRITE;
        }
        // Console proxy — everything under /api/*/console/*
        if (path.contains("/console/")) {
            return CallerPermission.CONSOLE_PROXY;
        }
        // MCP integration endpoints (if any) can be added here.
        return null;
    }

    /** Convenience: returns true iff the caller may serve the request. */
    public boolean isAuthorized(AllowedCaller caller, String method, String path) {
        CallerPermission required = requiredPermission(method, path);
        if (required == null) {
            return false;
        }
        return caller.hasPermission(required);
    }
}
