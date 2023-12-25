/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.core.rpc.netty;

import io.netty.channel.Channel;
import io.seata.core.rpc.netty.current.ProtocolTestConstants;

import java.util.concurrent.ConcurrentMap;

/**
 * Channel Manager Test Helper
 *
 * @author minghua.xie
 * @date 2023/12/25
 **/
public class ChannelManagerTestHelper {
    public static ConcurrentMap<String, Channel> getChannelConcurrentMap(AbstractNettyRemotingClient remotingClient) {
        return getChannelManager(remotingClient).getChannels();
    }

    public static Channel getChannel(TmNettyRemotingClient client) {
        return getChannelManager(client)
                .acquireChannel(ProtocolTestConstants.SERVER_ADDRESS);
    }
    private static NettyClientChannelManager getChannelManager(AbstractNettyRemotingClient remotingClient) {
        return remotingClient.getClientChannelManager();
    }
}
