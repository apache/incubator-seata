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
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.Version;
import org.apache.seata.core.protocol.VersionNotSupportMessage;
import org.apache.seata.core.protocol.transaction.UndoLogDeleteRequest;
import org.apache.seata.core.rpc.MsgVersionHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * MsgVersionHelper Test - Refactored to use BaseNettyClientTest
 */
public class MsgVersionHelperTest extends BaseNettyClientTest {

    @Test
    public void testSendMsgWithResponse() throws Exception {
        int dynamicPort = getDynamicPort();
        ServerInstance serverInstance = startServerSimple(dynamicPort);

        try {
            configureClient(dynamicPort);

            String applicationId = "app 1";
            String transactionServiceGroup = "default_tx_group";
            TmNettyRemotingClient tmNettyRemotingClient =
                    TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);
            tmNettyRemotingClient.init();

            Channel channel = TmNettyRemotingClient.getInstance()
                    .getClientChannelManager()
                    .acquireChannel(serverInstance.getAddress());

            RpcMessage rpcMessage = buildUndoLogDeleteMsg(ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY);
            Assertions.assertFalse(MsgVersionHelper.versionNotSupport(channel, rpcMessage));
            TmNettyRemotingClient.getInstance().sendAsync(channel, rpcMessage);

            Version.putChannelVersion(channel, "0.7.0");
            Assertions.assertTrue(MsgVersionHelper.versionNotSupport(channel, rpcMessage));
            TmNettyRemotingClient.getInstance().sendAsync(channel, rpcMessage);
            Object response = TmNettyRemotingClient.getInstance().sendSync(channel, rpcMessage, 100);
            Assertions.assertTrue(response instanceof VersionNotSupportMessage);

            tmNettyRemotingClient.destroy();
        } finally {
            serverInstance.destroy();
        }
    }

    private RpcMessage buildUndoLogDeleteMsg(byte messageType) {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(100);
        rpcMessage.setMessageType(messageType);
        rpcMessage.setCodec(ProtocolConstants.CONFIGURED_CODEC);
        rpcMessage.setCompressor(ProtocolConstants.CONFIGURED_COMPRESSOR);
        rpcMessage.setBody(new UndoLogDeleteRequest());
        return rpcMessage;
    }
}
