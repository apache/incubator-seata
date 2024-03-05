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

import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.Version;
import org.apache.seata.core.rpc.netty.v0.ProtocolEncoderV0;
import org.apache.seata.core.rpc.netty.v1.ProtocolEncoderV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Compatible Protocol Encoder
 * <p>
 * <li>Full Length: include all data </li>
 * <li>Head Length: include head data from magic code to head map. </li>
 * <li>Body Length: Full Length - Head Length</li>
 * </p>
 *
 */
public class CompatibleProtocolEncoder extends MessageToByteEncoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompatibleProtocolEncoder.class);

    private static Map<Byte, ProtocolEncoder> protocolEncoderMap;

    public CompatibleProtocolEncoder() {
        super();
        protocolEncoderMap = ImmutableMap.<Byte, ProtocolEncoder>builder()
                .put(ProtocolConstants.VERSION_0, new ProtocolEncoderV0())
                .put(ProtocolConstants.VERSION_1, new ProtocolEncoderV1())
                .build();
    }

    @Override
    public void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) {
        try {
            if (msg instanceof RpcMessage) {
                RpcMessage rpcMessage = (RpcMessage) msg;
                byte version = Version.calcProtocolVersion(rpcMessage.getVersion());
                ProtocolEncoder encoder = protocolEncoderMap.get(version);
                if (encoder == null) {
                    throw new UnsupportedOperationException("Unsupported version: " + version);
                }

                encoder.encode(rpcMessage, out);
            } else {
                throw new UnsupportedOperationException("Not support this class:" + msg.getClass());
            }
        } catch (Throwable e) {
            LOGGER.error("Encode request error!", e);
        }
    }
}
