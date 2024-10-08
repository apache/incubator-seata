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
package org.apache.seata.core.rpc.netty.v1;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.seata.core.rpc.netty.ProtocolEncoder;
import org.apache.seata.core.serializer.Serializer;
import org.apache.seata.core.compressor.Compressor;
import org.apache.seata.core.compressor.CompressorFactory;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.serializer.SerializerServiceLoader;
import org.apache.seata.core.serializer.SerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * <pre>
 * 0     1     2     3     4     5     6     7     8     9    10     11    12    13    14    15    16
 * +-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+
 * |   magic   |Proto|     Full length       |    Head   | Msg |Seria|Compr|     RequestId         |
 * |   code    |colVer|    (head+body)      |   Length  |Type |lizer|ess  |                       |
 * +-----------+-----------+-----------+-----------+-----------+-----------+-----------+-----------+
 * |                                                                                               |
 * |                                   Head Map [Optional]                                         |
 * +-----------+-----------+-----------+-----------+-----------+-----------+-----------+-----------+
 * |                                                                                               |
 * |                                         body                                                  |
 * |                                                                                               |
 * |                                        ... ...                                                |
 * +-----------------------------------------------------------------------------------------------+
 * </pre>
 * <p>
 * <li>Full Length: include all data </li>
 * <li>Head Length: include head data from magic code to head map. </li>
 * <li>Body Length: Full Length - Head Length</li>
 * </p>
 * https://github.com/seata/seata/issues/893
 *
 * @author Geng Zhang
 * @see ProtocolDecoderV1
 * @since 0.7.0
 */
public class ProtocolEncoderV1 extends MessageToByteEncoder implements ProtocolEncoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolEncoderV1.class);


    public void encode(RpcMessage message, ByteBuf out) {
        try {
            ProtocolRpcMessageV1 rpcMessage = new ProtocolRpcMessageV1();
            rpcMessage.rpcMsg2ProtocolMsg(message);

            int fullLength = ProtocolConstants.V1_HEAD_LENGTH;
            int headLength = ProtocolConstants.V1_HEAD_LENGTH;

            byte messageType = rpcMessage.getMessageType();
            out.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
            out.writeByte(ProtocolConstants.VERSION_1);
            // full Length(4B) and head length(2B) will fix in the end.
            out.writerIndex(out.writerIndex() + 6);
            out.writeByte(messageType);
            out.writeByte(rpcMessage.getCodec());
            out.writeByte(rpcMessage.getCompressor());
            out.writeInt(rpcMessage.getId());

            // direct write head with zero-copy
            Map<String, String> headMap = rpcMessage.getHeadMap();
            if (headMap != null && !headMap.isEmpty()) {
                int headMapBytesLength = HeadMapSerializer.getInstance().encode(headMap, out);
                headLength += headMapBytesLength;
                fullLength += headMapBytesLength;
            }

            byte[] bodyBytes = null;
            if (messageType != ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST
                && messageType != ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE) {
                // heartbeat has no body
                Serializer serializer = SerializerServiceLoader.load(SerializerType.getByCode(rpcMessage.getCodec()), ProtocolConstants.VERSION_1);
                bodyBytes = serializer.serialize(rpcMessage.getBody());
                Compressor compressor = CompressorFactory.getCompressor(rpcMessage.getCompressor());
                bodyBytes = compressor.compress(bodyBytes);
                fullLength += bodyBytes.length;
            }

            if (bodyBytes != null) {
                out.writeBytes(bodyBytes);
            }

            // fix fullLength and headLength
            int writeIndex = out.writerIndex();
            // skip magic code(2B) + version(1B)
            out.writerIndex(writeIndex - fullLength + 3);
            out.writeInt(fullLength);
            out.writeShort(headLength);
            out.writerIndex(writeIndex);


        } catch (Throwable e) {
            LOGGER.error("Encode request error!", e);
            // todo
            throw e;
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        try {
            if (msg instanceof RpcMessage) {
                this.encode((RpcMessage)msg, out);
            } else {
                throw new UnsupportedOperationException("Not support this class:" + msg.getClass());
            }
        } catch (Throwable e) {
            LOGGER.error("Encode request error!", e);
        }
    }

}
