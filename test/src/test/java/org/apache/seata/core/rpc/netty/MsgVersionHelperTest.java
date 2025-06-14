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
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.ConfigurationTestHelper;
import org.apache.seata.common.XID;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.UUIDGenerator;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.Version;
import org.apache.seata.core.protocol.VersionNotSupportMessage;
import org.apache.seata.core.protocol.transaction.UndoLogDeleteRequest;
import org.apache.seata.core.rpc.MsgVersionHelper;
import org.apache.seata.server.coordinator.DefaultCoordinator;
import org.apache.seata.server.session.SessionHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * MsgVersionHelper Test
 **/
public class MsgVersionHelperTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MsgVersionHelperTest.class);

    @BeforeAll
    public static void init(){
        ConfigurationTestHelper.putConfig(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL, "8091");
    }
    @AfterAll
    public static void after() {
        ConfigurationTestHelper.removeConfig(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
    }

    public static ThreadPoolExecutor initMessageExecutor() {
        return new ThreadPoolExecutor(5, 5, 500, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(20000), new ThreadPoolExecutor.CallerRunsPolicy());
    }
    @Test
    public void testSendMsgWithResponse() throws Exception {
        ThreadPoolExecutor workingThreads = initMessageExecutor();
        NettyRemotingServer nettyRemotingServer = new NettyRemotingServer(workingThreads);
        new Thread(() -> {
            SessionHolder.init(null);
            nettyRemotingServer.setHandler(DefaultCoordinator.getInstance(nettyRemotingServer));
            // set registry
            XID.setIpAddress(NetUtil.getLocalIp());
            XID.setPort(8091);
            // init snowflake for transactionId, branchId
            UUIDGenerator.init(1L);
            nettyRemotingServer.init();
        }).start();
        Thread.sleep(3000);

        String applicationId = "app 1";
        String transactionServiceGroup = "default_tx_group";
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);
        tmNettyRemotingClient.init();

        String serverAddress = "0.0.0.0:8091";
        Channel channel = TmNettyRemotingClient.getInstance().getClientChannelManager().acquireChannel(serverAddress);

        RpcMessage rpcMessage = buildUndoLogDeleteMsg(ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY);
        Assertions.assertFalse(MsgVersionHelper.versionNotSupport(channel, rpcMessage));
        TmNettyRemotingClient.getInstance().sendAsync(channel,rpcMessage);


        Version.putChannelVersion(channel,"0.7.0");
        Assertions.assertTrue(MsgVersionHelper.versionNotSupport(channel, rpcMessage));
        TmNettyRemotingClient.getInstance().sendAsync(channel,rpcMessage);
        Object response = TmNettyRemotingClient.getInstance().sendSync(channel, rpcMessage, 100);
        Assertions.assertTrue(response instanceof VersionNotSupportMessage);

        nettyRemotingServer.destroy();
        tmNettyRemotingClient.destroy();
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
