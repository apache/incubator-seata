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
package org.apache.seata.core.rpc.netty.v0;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.MessageTypeAware;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.rpc.netty.ProtocolEncoder;
import org.apache.seata.core.serializer.Serializer;
import org.apache.seata.core.serializer.SerializerServiceLoader;
import org.apache.seata.core.serializer.SerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 *  seata-version < 0.7
 *  Only used in TC send a request to RM/TM.
 * 0     1     2     3     4           6           8          10           12          14         16
 * +-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+
 * |   0xdada  |   flag    | typecode/ |                 requestid                     |           |
 * |           |           | bodylength|                                               |           |
 * +-----------+-----------+-----------+-----------+-----------+-----------+-----------+           +
 * |                                    ... ...                                                    |
 * +                                                                                               +
 * |                                     body                                                      |
 * +                                                                                               +
 * |                                    ... ...                                                    |
 * +-----------------------------------------------------------------------------------------------+
 *
 * </pre>
 * <p>
 * <li>flag: msg type </li>
 * <li>typecode: action type code </li>
 * <li>bodylength: body Length </li>
 * <li>requestid: request id</li>
 * </p>
 *
 * @see ProtocolDecoderV0
 */
public class ProtocolEncoderV0 extends MessageToByteEncoder implements ProtocolEncoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolEncoderV0.class);

    @Override
    public void encode(RpcMessage message, ByteBuf out) {
        try {
            byte codec = message.getCodec();
            ProtocolRpcMessageV0 msg = new ProtocolRpcMessageV0();
            msg.rpcMsg2ProtocolMsg(message);

            out.writeShort(ProtocolConstantsV0.MAGIC);
            int flag = (msg.isAsync() ? ProtocolConstantsV0.FLAG_ASYNC : 0)
                | (msg.isHeartbeat() ? ProtocolConstantsV0.FLAG_HEARTBEAT : 0)
                | (msg.isRequest() ? ProtocolConstantsV0.FLAG_REQUEST : 0)
                | (msg.isSeataCodec() ? ProtocolConstantsV0.FLAG_SEATA_CODEC : 0);

            out.writeShort((short) flag);

            if (msg.getBody() instanceof HeartbeatMessage) {
                out.writeShort((short) 0);
                out.writeLong(msg.getId());
                return;
            }

            byte[] bodyBytes = null;
            Serializer serializer = SerializerServiceLoader.load(SerializerType.getByCode(codec), ProtocolConstants.VERSION_0);
            bodyBytes = serializer.serialize(msg.getBody());

            if (msg.isSeataCodec()) {
                if (msg.getBody() instanceof MessageTypeAware) {
                    short typeCode = ((MessageTypeAware) msg.getBody()).getTypeCode();
                    out.writeShort(typeCode);
                }
            } else {
                out.writeShort(bodyBytes.length);
            }
            out.writeLong(msg.getId());
            if (bodyBytes != null) {
                out.writeBytes(bodyBytes);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Send:" + msg.getBody());
            }
        } catch (Throwable e) {
            LOGGER.error("Encode request error!", e);
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        try {
            if (msg instanceof RpcMessage) {
                encode((RpcMessage)msg, out);
            } else {
                throw new UnsupportedOperationException("Not support this class:" + msg.getClass());
            }
        } catch (Throwable e) {
            LOGGER.error("Encode request error!", e);
        }
    }
}
