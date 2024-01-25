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
package org.apache.seata.mockserver.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import org.apache.seata.core.protocol.AbstractMessage;
import org.apache.seata.core.protocol.AbstractResultMessage;
import org.apache.seata.core.protocol.MergeResultMessage;
import org.apache.seata.core.protocol.MergedWarpMessage;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.core.rpc.RpcContext;
import org.apache.seata.core.rpc.TransactionMessageHandler;
import org.apache.seata.core.rpc.netty.ChannelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock Remoting Processor
 **/
public class MockOnReqProcessor extends MockRemotingProcessor {
    protected static final Logger LOGGER = LoggerFactory.getLogger(MockOnReqProcessor.class);


    public MockOnReqProcessor(RemotingServer remotingServer, TransactionMessageHandler handler) {
        super(remotingServer, handler);
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        super.process(ctx, rpcMessage);
        Object message = rpcMessage.getBody();

        RpcContext rpcContext = ChannelManager.getContextFromIdentified(ctx.channel());

        // the batch send request message
        if (message instanceof MergedWarpMessage) {
            MergedWarpMessage mmsg = (MergedWarpMessage) message;
            MergeResultMessage resultMessage = new MergeResultMessage();
            List<AbstractResultMessage> resList = new ArrayList<>();
            for (int i = 0; i < mmsg.msgs.size(); i++) {
                AbstractMessage msg = mmsg.msgs.get(i);
                resList.add(handler.onRequest(msg, rpcContext));
            }
            AbstractResultMessage[] resultMsgs = Arrays.copyOf(resList.toArray(), resList.size(), AbstractResultMessage[].class);
            resultMessage.setMsgs(resultMsgs);
            remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), resultMessage);
            LOGGER.info("sendAsyncResponse: {}", resultMessage);
        } else {
            final AbstractMessage msg = (AbstractMessage) message;
            AbstractResultMessage result = handler.onRequest(msg, rpcContext);
            remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), result);
            LOGGER.info("sendAsyncResponse: {}", result);
        }
    }


}
