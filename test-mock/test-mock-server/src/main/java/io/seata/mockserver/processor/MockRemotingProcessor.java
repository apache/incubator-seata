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
package io.seata.mockserver.processor;

import io.netty.channel.ChannelHandlerContext;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.RemotingServer;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.processor.RemotingProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock Remoting Processor
 *
 * @author minghua.xie
 * @date 2023/11/14
 **/
public class MockRemotingProcessor implements RemotingProcessor {

    protected static final Logger LOGGER = LoggerFactory.getLogger(MockRemotingProcessor.class);
    protected RemotingServer remotingServer;
    protected final TransactionMessageHandler handler;


    public MockRemotingProcessor(RemotingServer remotingServer, TransactionMessageHandler handler) {
        this.remotingServer = remotingServer;
        this.handler = handler;
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        Object message = rpcMessage.getBody();
        LOGGER.info("process message : " + message);

    }


}
