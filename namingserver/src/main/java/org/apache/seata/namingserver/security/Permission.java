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

/**
 * Discrete permissions a caller can hold. Kept as a small closed set so audit / config
 * remains understandable — we deliberately do not model arbitrary RBAC here.
 */
public enum Permission {
    /** Register a TC instance (initial admission). */
    REGISTER,
    /** Send heartbeat (re-register) for an already-registered TC. */
    HEARTBEAT,
    /** Modify the vGroup → cluster mapping via the console. */
    VGROUP_WRITE,
    /** Read cluster / namespace / vgroup data via the console. */
    CONSOLE_READ,
    /** Perform write operations (force commit/rollback etc.) via the console proxy. */
    CONSOLE_WRITE,
    /** Called from the MCP integration; grants API-level access without console UI context. */
    MCP
}
