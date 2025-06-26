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
package org.apache.seata.core.rpc.netty;

import io.netty.channel.Channel;

/**
 * The interface Channel event listener.
 */
public interface ChannelEventListener {
    /**
     * On channel connect.
     *
     * @param channel    the channel
     */
    default void onChannelConnected(final Channel channel) {}

    /**
     * On channel close.
     *
     * @param channel    the channel
     */
    default void onChannelDisconnected(final Channel channel) {}

    /**
     * On channel exception.
     *
     * @param channel    the channel
     * @param cause      the cause
     */
    default void onChannelException(final Channel channel, Throwable cause) {}

    /**
     * On channel idle.
     *
     * @param channel    the channel
     */
    default void onChannelIdle(final Channel channel) {}
}
