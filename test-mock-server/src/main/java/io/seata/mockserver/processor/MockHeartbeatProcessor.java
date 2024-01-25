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
package io.seata.mockserver.processor;

import io.netty.channel.ChannelHandlerContext;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.RemotingServer;
import io.seata.core.rpc.TransactionMessageHandler;

/**
 * Mock Heartbeat Processor
 **/
public class MockHeartbeatProcessor extends MockRemotingProcessor {
    public MockHeartbeatProcessor(RemotingServer remotingServer, TransactionMessageHandler handler) {
        super(remotingServer, handler);
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        super.process(ctx, rpcMessage);
        remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), HeartbeatMessage.PONG);
    }
}
