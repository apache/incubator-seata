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

import java.util.concurrent.ConcurrentMap;

import io.netty.channel.Channel;
import org.apache.seata.core.rpc.netty.mockserver.ProtocolTestConstants;

/**
 * Channel Manager Test Helper
 *
 **/
public class ChannelManagerTestHelper {
    public static ConcurrentMap<String, Channel> getChannelConcurrentMap(AbstractNettyRemotingClient remotingClient) {
        return getChannelManager(remotingClient).getChannels();
    }

    public static Channel getChannel(TmNettyRemotingClient client) {
        return getChannelManager(client)
                .acquireChannel(ProtocolTestConstants.MOCK_SERVER_ADDRESS);
    }
    private static NettyClientChannelManager getChannelManager(AbstractNettyRemotingClient remotingClient) {
        return remotingClient.getClientChannelManager();
    }
}
