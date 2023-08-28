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

import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.seata.core.protocol.ProtocolConstants;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.netty.v0.ProtocolV0Encoder;
import io.seata.core.rpc.netty.v1.ProtocolV1Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * <pre>
 * </pre>
 * <p>
 * <li>Full Length: include all data </li>
 * <li>Head Length: include head data from magic code to head map. </li>
 * <li>Body Length: Full Length - Head Length</li>
 * </p>
 * https://github.com/seata/seata/issues/893
 *
 * @author Bughue
 * @since 2.0.0
 */
public class CompatibleProtocolEncoder extends MessageToByteEncoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompatibleProtocolEncoder.class);

    private static Map<Byte, ProtocolEncoder> protocolEncoderMap;

    public CompatibleProtocolEncoder(){
        super();
        protocolEncoderMap = ImmutableMap.<Byte, ProtocolEncoder>builder()
                .put(ProtocolConstants.VERSION_0, new ProtocolV0Encoder())
                .put(ProtocolConstants.VERSION_1, new ProtocolV1Encoder())
                .build();
    }

    @Override
    public void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) {
        try {
            if (msg instanceof RpcMessage) {
                RpcMessage rpcMessage = (RpcMessage) msg;
                byte version = rpcMessage.getProtocolVersion();
                ProtocolEncoder encoder = protocolEncoderMap.get(version);
                if (encoder == null) {
                    // todo [5738-discuss][encode] 要不要适配当前版本？
                    throw new IllegalArgumentException("Unknown version: " + version);
                }

                encoder.encode(rpcMessage,out);
            } else {
                throw new UnsupportedOperationException("Not support this class:" + msg.getClass());
            }
        } catch (Throwable e) {
            LOGGER.error("Encode request error!", e);
        }
    }
}
