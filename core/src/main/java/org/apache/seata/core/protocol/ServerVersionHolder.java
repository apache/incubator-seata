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

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.apache.seata.common.util.StringUtils;

/**
 * Holds the version of the server (TC) at the other end of a client channel.
 * <p>
 * The server always echoes its own version back in the register response
 * ({@link AbstractIdentifyResponse#getVersion()}). This holder keeps that version so that upper
 * layers can decide whether a feature is supported by the target server before sending a request,
 * which is the client side counterpart of {@code RpcContext#getVersion()} on the server side.
 * <p>
 * The version is bound to the registered channel itself (as a channel attribute), NOT to the
 * server address nor to the remote address of the channel:
 * <ul>
 *     <li>a server address may be a Kubernetes Service or a TCP load balancer, so the TM channel and
 *     the RM channel to the same address can reach different TC instances, possibly running
 *     different versions during a rolling upgrade; each channel only ever reports the version of the
 *     TC it is actually connected to;</li>
 *     <li>a reconnected channel starts without any version, so it can never inherit the version of
 *     a previous connection to the same address. The rpc layer records the version before it
 *     publishes the channel, and an unknown version is treated as not supported;</li>
 *     <li>the version lives and dies with its channel, so no cleanup or client lifecycle
 *     coordination is required, and destroying a client can never affect the channels of another
 *     (or a newly created) client.</li>
 * </ul>
 *
 * @since 2.6.0
 */
public final class ServerVersionHolder {

    private static final AttributeKey<String> SERVER_VERSION_KEY =
            AttributeKey.valueOf(ServerVersionHolder.class.getName() + ".SERVER_VERSION");

    private ServerVersionHolder() {}

    /**
     * Record the version of the server the channel is connected to. Should only be called by the rpc
     * layer once the registration on that channel succeeds, and before the channel is published.
     *
     * @param channel the registered channel
     * @param version the version reported by the server in its register response
     */
    public static void putServerVersion(Channel channel, String version) {
        if (channel == null || StringUtils.isBlank(version)) {
            return;
        }
        Attribute<String> attribute = channel.attr(SERVER_VERSION_KEY);
        if (attribute != null) {
            attribute.set(version);
        }
    }

    /**
     * Gets the version of the server the channel is connected to.
     *
     * @param channel the channel to the server
     * @return the server version, null if the channel has not been registered successfully
     */
    public static String getServerVersion(Channel channel) {
        if (channel == null) {
            return null;
        }
        Attribute<String> attribute = channel.attr(SERVER_VERSION_KEY);
        return attribute == null ? null : attribute.get();
    }

    /**
     * Determine whether the server the channel is connected to is above or equal to the given
     * version. An unknown server version is treated as not satisfied, so that a feature relying on
     * this check stays disabled until the version is really known.
     *
     * @param channel       the channel to the server
     * @param targetVersion the version to compare with
     * @return true if the server version is known and above or equal to the target version
     */
    public static boolean isServerAboveOrEqualVersion(Channel channel, String targetVersion) {
        String serverVersion = getServerVersion(channel);
        if (StringUtils.isBlank(serverVersion) || StringUtils.isBlank(targetVersion)) {
            return false;
        }
        return Version.isAboveOrEqualVersion(serverVersion, targetVersion);
    }
}
