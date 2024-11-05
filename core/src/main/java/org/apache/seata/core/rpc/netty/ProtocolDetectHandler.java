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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.seata.core.protocol.detector.Http2Detector;
import org.apache.seata.core.protocol.detector.ProtocolDetector;
import org.apache.seata.core.protocol.detector.SeataDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProtocolDetectHandler extends ByteToMessageDecoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolDetectHandler.class);
    private NettyServerBootstrap nettyServerBootstrap;
    private ProtocolDetector[] supportedProtocolDetectors;

    public ProtocolDetectHandler(NettyServerBootstrap nettyServerBootstrap) {
        this.nettyServerBootstrap = nettyServerBootstrap;
        this.supportedProtocolDetectors = new ProtocolDetector[]{new Http2Detector(nettyServerBootstrap.getChannelHandlers()), new SeataDetector(nettyServerBootstrap.getChannelHandlers())};
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        for (ProtocolDetector protocolDetector : supportedProtocolDetectors) {
            if (protocolDetector.detect(in)) {
                ChannelHandler[] protocolHandlers = protocolDetector.getHandlers();
                ctx.pipeline().addLast(protocolHandlers);
                ctx.pipeline().remove(this);

                in.resetReaderIndex();
                return;
            }

            in.resetReaderIndex();
        }

        byte[] preface = new byte[in.readableBytes()];
        in.readBytes(preface);
        LOGGER.error("Can not recognize protocol from remote {}, preface = {}", ctx.channel().remoteAddress(), preface);
        in.clear();
        ctx.close();
    }
}