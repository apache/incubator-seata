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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.seata.core.protocol.RpcMessage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test client handler that captures server responses.
 * Supports multiple request/response cycles using resetLatch().
 */
public class TestClientHandler extends ChannelInboundHandlerAdapter {

    private final AtomicReference<Object> responseRef;
    private volatile CountDownLatch responseLatch;

    public TestClientHandler(AtomicReference<Object> responseRef, CountDownLatch responseLatch) {
        this.responseRef = responseRef;
        this.responseLatch = responseLatch;
    }

    public TestClientHandler() {
        this.responseRef = new AtomicReference<>();
        this.responseLatch = new CountDownLatch(1);
    }

    /**
     * Reset the latch for a new request/response cycle
     */
    public void resetLatch(CountDownLatch newLatch) {
        this.responseLatch = newLatch;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Handle RpcMessage wrapped responses
        if (msg instanceof RpcMessage) {
            RpcMessage rpcMessage = (RpcMessage) msg;
            Object body = rpcMessage.getBody();
            responseRef.set(body);
        } else {
            // Handle direct responses (for backward compatibility)
            responseRef.set(msg);
        }
        if (responseLatch != null) {
            responseLatch.countDown();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
