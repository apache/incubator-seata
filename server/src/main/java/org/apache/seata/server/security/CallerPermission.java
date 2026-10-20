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
 * Permissions accepted by the Seata Server inbound auth filter. Smaller vocabulary than the
 * NamingServer side because TC only ever receives three <em>categories</em> of inbound calls
 * from NamingServer: vGroup writes, console proxy pass-through, and MCP integration calls.
 */
public enum CallerPermission {
    /** May call {@code /vgroup/v1/addVGroup} and {@code /vgroup/v1/removeVGroup}. */
    VGROUP_WRITE,
    /** May call any {@code /api/*} console-facing endpoint (proxied by NamingServer). */
    CONSOLE_PROXY,
    /** Reserved for MCP-driven admin integrations. */
    MCP
}
