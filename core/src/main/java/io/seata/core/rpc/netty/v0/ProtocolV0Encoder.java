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
package io.seata.core.rpc.netty.v0;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.MessageTypeAware;
import io.seata.core.protocol.ProtocolConstants;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.netty.ProtocolEncoder;
import io.seata.core.serializer.Serializer;
import io.seata.core.serializer.SerializerServiceLoader;
import io.seata.core.serializer.SerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

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
 * @author Bughue
 * @see ProtocolV0Decoder
 * @since 2.0.0
 */
public class ProtocolV0Encoder implements ProtocolEncoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolV0Encoder.class);

    @Override
    public void encode(RpcMessage message, ByteBuf out) {
        try {
            byte codec = message.getCodec();
            ProtocolV0RpcMessage msg = new ProtocolV0RpcMessage();
            msg.rpcMsg2ProtocolMsg(message);

            out.writeShort(ProtocolV0Constants.MAGIC);
            int flag = (msg.isAsync() ? ProtocolV0Constants.FLAG_ASYNC : 0)
                    | (msg.isHeartbeat() ? ProtocolV0Constants.FLAG_HEARTBEAT : 0)
                    | (msg.isRequest() ? ProtocolV0Constants.FLAG_REQUEST : 0)
                    | (msg.isSeataCodec() ? ProtocolV0Constants.FLAG_SEATA_CODEC : 0);

            out.writeShort((short) flag);

            if (msg.getBody() instanceof HeartbeatMessage) {
                out.writeShort((short) 0);
                out.writeLong(msg.getId());
                return;
            }

            byte[] bodyBytes = null;
            Serializer serializer = SerializerServiceLoader.load(SerializerType.getByCode(codec), ProtocolConstants.VERSION_0);
            bodyBytes = serializer.serialize(msg.getBody());

            if(msg.isSeataCodec()){
                if(msg.getBody() instanceof MessageTypeAware){
                    short typeCode = ((MessageTypeAware) msg.getBody()).getTypeCode();
                    out.writeShort(typeCode);
                }
            }else {
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
}
