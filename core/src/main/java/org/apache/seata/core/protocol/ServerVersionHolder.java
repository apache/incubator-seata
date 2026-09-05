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
package org.apache.seata.core.protocol;

import org.apache.seata.common.util.StringUtils;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the version of each server (TC) the client has registered to.
 * <p>
 * The server always echoes its own version back in the register response
 * ({@link AbstractIdentifyResponse#getVersion()}). This holder keeps that version so that upper
 * layers can decide whether a feature is supported by the target server before sending a request,
 * which is the client side counterpart of {@code RpcContext#getVersion()} on the server side.
 * <p>
 * Note that the key is the server address used by the client to connect (the same key as
 * {@code NettyClientChannelManager#channels}), NOT the resolved remote address of a channel, so it
 * must not be mixed up with {@link Version#getChannelVersion(io.netty.channel.Channel)}.
 * <p>
 * Entries are not removed when a single channel disconnects, and that is intentional: TM and RM
 * connect to the same servers through two independent channel managers, so dropping an entry when
 * one of them disconnects would break feature detection for the other one. A version is an
 * intrinsic attribute of the server at a given address and is overwritten on every successful
 * registration, so a stale entry can never be read through a live channel, since such a channel can
 * only exist after a registration that has just refreshed the entry. A caller that looks a version
 * up by address without holding a channel to that server is not covered by this and has to handle a
 * stale or absent version itself.
 * <p>
 * The recorded versions are discarded once every client that could read them has been destroyed,
 * see {@link #attach(String)} and {@link #detach(String)}, so the map does not outlive the clients
 * that populated it.
 *
 * @since 2.6.0
 */
public class ServerVersionHolder {

    private static final Map<String, String> SERVER_VERSION_MAP = new ConcurrentHashMap<>();

    private static final Set<String> ACTIVE_CLIENTS = ConcurrentHashMap.newKeySet();

    private ServerVersionHolder() {}

    /**
     * Mark a client as active. Should be called by a client once it is initialized, so that the
     * recorded versions are kept as long as that client may read them.
     *
     * @param clientRole the role of the client, that is the name of a
     *                   {@code NettyPoolKey.TransactionRole}
     */
    public static void attach(String clientRole) {
        if (StringUtils.isBlank(clientRole)) {
            return;
        }
        ACTIVE_CLIENTS.add(clientRole);
    }

    /**
     * Mark a client as destroyed, and discard the recorded versions once no client is left. This is
     * the only point at which entries are removed, see the class javadoc for why a single channel
     * disconnection must not remove them.
     *
     * @param clientRole the role of the client, that is the name of a
     *                   {@code NettyPoolKey.TransactionRole}
     */
    public static void detach(String clientRole) {
        if (StringUtils.isBlank(clientRole)) {
            return;
        }
        if (ACTIVE_CLIENTS.remove(clientRole) && ACTIVE_CLIENTS.isEmpty()) {
            clear();
        }
    }

    /**
     * Record the version of the server. Should only be called by the rpc layer once the
     * registration to the server succeeds.
     *
     * @param serverAddress the server address
     * @param version       the version reported by the server
     */
    public static void putServerVersion(String serverAddress, String version) {
        if (StringUtils.isBlank(serverAddress) || StringUtils.isBlank(version)) {
            return;
        }
        SERVER_VERSION_MAP.put(serverAddress, version);
    }

    /**
     * Gets the version of the server.
     *
     * @param serverAddress the server address
     * @return the server version, null if the client has never registered to that server
     */
    public static String getServerVersion(String serverAddress) {
        if (StringUtils.isBlank(serverAddress)) {
            return null;
        }
        return SERVER_VERSION_MAP.get(serverAddress);
    }

    /**
     * Determine whether the server is above or equal to the given version. An unknown server
     * version is treated as not satisfied, so that a feature relying on this check stays disabled
     * until the version is really known.
     *
     * @param serverAddress  the server address
     * @param targetVersion  the version to compare with
     * @return true if the server version is known and above or equal to the target version
     */
    public static boolean isServerAboveOrEqualVersion(String serverAddress, String targetVersion) {
        String serverVersion = getServerVersion(serverAddress);
        if (StringUtils.isBlank(serverVersion) || StringUtils.isBlank(targetVersion)) {
            return false;
        }
        return Version.isAboveOrEqualVersion(serverVersion, targetVersion);
    }

    /**
     * Clear all recorded server versions.
     */
    public static void clear() {
        SERVER_VERSION_MAP.clear();
    }
}
