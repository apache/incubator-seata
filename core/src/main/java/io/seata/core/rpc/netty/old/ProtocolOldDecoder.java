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
package io.seata.core.rpc.netty.old;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.seata.core.compressor.Compressor;
import io.seata.core.compressor.CompressorFactory;
import io.seata.core.compressor.CompressorType;
import io.seata.core.exception.DecodeException;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.ProtocolConstants;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.netty.CompatibleProtocolDecoder;
import io.seata.core.rpc.netty.v1.HeadMapSerializer;
import io.seata.core.rpc.netty.v1.ProtocolV1Encoder;
import io.seata.core.serializer.Serializer;
import io.seata.core.serializer.SerializerServiceLoader;
import io.seata.core.serializer.SerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * <pre>
 *  seata-version < 0.7
 *  Only used in TC receives a request from RM/TM.
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
 * @author minghua.xie
 * @see ProtocolOldEncoder
 * @since 2.0.0
 */
public class ProtocolOldDecoder {



    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolOldDecoder.class);


    public static RpcMessage decodeFrame(ByteBuf frame) {
        // todo 旧版本是直接返回跳过了，我们需要保留这个逻辑？
//        if (frame.readableBytes() < HEAD_LENGTH) {
//            return;
//        }

        // todo 这里是为了bodyLength不满足要求时reset，意义是什么？
        frame.markReaderIndex();

        // todo 外层已经判断过了，这里可以跳过
        short protocol = frame.readShort();

        int flag = (int)frame.readShort();

        boolean isHeartbeat = (ProtocolOldConstants.FLAG_HEARTBEAT & flag) > 0;
        boolean isRequest = (ProtocolOldConstants.FLAG_REQUEST & flag) > 0;
        boolean isSeataCodec = (ProtocolOldConstants.FLAG_SEATA_CODEC & flag) > 0;

        // todo 目前看来，旧协议不是seata就是hessian
        byte codecType = isSeataCodec?SerializerType.SEATA.getCode():SerializerType.HESSIAN.getCode();

        short bodyLength = 0;
        short typeCode = 0;
        if (!isSeataCodec) { bodyLength = frame.readShort(); } else { typeCode = frame.readShort(); }
        long msgId = frame.readLong();
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setCompressor(CompressorType.NONE.getCode());
        rpcMessage.setCodec(codecType);

        // todo reqid之前是long的，设置不进去，要把rpcMessage也隔离成两套吗？
//        rpcMessage.setId(msgId);
        if (isHeartbeat) {
            // todo MessageType是新字段，以前其实分了两个字段，一个是isHeartbeat，一个是isRequest
            if (isRequest) {
                rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST);
                rpcMessage.setBody(HeartbeatMessage.PING);
            } else {
                rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE);
                rpcMessage.setBody(HeartbeatMessage.PONG);
            }
            return rpcMessage;
        }

        if (bodyLength > 0 && frame.readableBytes() < bodyLength) {
            frame.resetReaderIndex();
            return rpcMessage;
        }

        // TODO  怎样知道是不是one way
        rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_RESQUEST_SYNC);
        rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY);

        rpcMessage.setBody(CompatibleProtocolDecoder.getBody(frame, rpcMessage.getMessageType(),
                rpcMessage.getCompressor(),rpcMessage.getCodec(),bodyLength));
        return rpcMessage;
    }

    private static short getShort(byte b0, byte b1) {
        ByteBuffer byteBufferX = ByteBuffer.allocate(128);
        byteBufferX.put(b0);
        byteBufferX.put(b1);
        return byteBufferX.getShort(0);
    }
}
