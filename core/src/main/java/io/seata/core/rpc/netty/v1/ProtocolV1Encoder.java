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
package io.seata.core.rpc.netty.v1;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.seata.core.codec.Codec;
import io.seata.core.codec.CodecFactory;
import io.seata.core.protocol.ProtocolConstants;
import io.seata.core.protocol.RpcMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * <p>
 * 0     1     2     3     4     5     6     7     8     9    10     11    12    13    14    15    16
 * +-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+
 * |   magic   |Proto|     Full length       |  HeadMap  | Msg |Seria|Compr|     RequestId         |
 * |   code    |colVer|    （head+body)      |   Length  |Type |lizer|ess  |                       |
 * +-----------+-----------+-----------+-----------+-----------+-----------+-----------+-----------+
 * |                                                                                               |
 * |                                   Head Map [Optional]                                         |
 * +-----------+-----------+-----------+-----------+-----------+-----------+-----------+-----------+
 * |                                                                                               |
 * |                                         body                                                  |
 * |                                                                                               |
 * |                                        ... ...                                                |
 * +-----------------------------------------------------------------------------------------------+
 * </p>
 * <p>
 * https://github.com/seata/seata/issues/893
 *
 * @author Geng Zhang
 * @since 0.7.0
 */
public class ProtocolV1Encoder extends MessageToByteEncoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolV1Encoder.class);

    @Override
    public void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) {
        try {
            if (msg instanceof RpcMessage) {
                RpcMessage rpcMessage = (RpcMessage) msg;

                int fullLength = 13;
                int headLength = 0;

                // count head
                byte[] headBytes = null;
                Map<String, String> headMap = rpcMessage.getHeadMap();
                if (headMap != null && !headMap.isEmpty()) {
                    headBytes = HeadMapSerializer.getInstance().encode(headMap);
                    headLength = headBytes.length;
                    fullLength += headLength;
                }

                byte[] bodyBytes = null;
                byte messageType = rpcMessage.getMessageType();
                if (messageType != ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST
                        && messageType != ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE) {
                    // heartbeat has no body
                    Codec codec = CodecFactory.getCodec(rpcMessage.getCodec());
                    bodyBytes = codec.encode(rpcMessage.getBody());
                    fullLength += bodyBytes.length;
                }

                out.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
                out.writeByte(ProtocolConstants.VERSION);
                out.writeInt(fullLength);
                out.writeShort(headLength);
                out.writeByte(messageType);
                out.writeByte(rpcMessage.getCodec());
                out.writeByte(rpcMessage.getCompressor());
                out.writeInt(rpcMessage.getId());

                if (headBytes != null) {
                    out.writeBytes(headBytes);
                }

                if (bodyBytes != null) {
                    out.writeBytes(bodyBytes);
                }
            } else {
                throw new UnsupportedOperationException("Not support this class:" + msg.getClass());
            }
        } catch (Throwable e) {
            LOGGER.error("Encode request error!", e);
        } 
    }
}
