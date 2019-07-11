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
import io.seata.core.codec.Codec;
import io.seata.core.codec.CodecFactory;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.ProtocolConstants;
import io.seata.core.protocol.RpcMessage;

import java.io.IOException;
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
 * <p>
 * https://github.com/seata/seata/issues/893
 *
 * @author Geng Zhang
 * @since 0.7.0
 */
public class ProtocolV1Decoder extends io.netty.handler.codec.LengthFieldBasedFrameDecoder {

    public ProtocolV1Decoder() {
        // default is 8M
        this(8 * 1024 * 1024);
    }

    public ProtocolV1Decoder(int maxFrameLength) {
        /*
        int maxFrameLength,      
        int lengthFieldOffset,  magic code is 2B, and version is 1B, and then FullLength. so value is 3
        int lengthFieldLength,  FullLength is int(4B). so values is 4
        int lengthAdjustment,   FullLength include 4 bytes self, so the left length is (FullLength-4). so values is -4
        int initialBytesToStrip we will check magic code and version self, so do not strip any bytes. so values is                                                                                                          0
        */
        super(maxFrameLength, 3, 4, -4, 0);
    }

    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        Object decoded = super.decode(ctx, in);
        if (decoded instanceof ByteBuf) {
            ByteBuf frame = (ByteBuf) decoded;
            return decodeFrame(frame);
        }
        return decoded;
    }

    public Object decodeFrame(ByteBuf frame) throws IOException {
        byte b0 = frame.readByte();
        byte b1 = frame.readByte();
        if (ProtocolConstants.MAGIC_CODE_BYTES[0] != b0
                || ProtocolConstants.MAGIC_CODE_BYTES[1] != b1) {
            throw new IllegalArgumentException("Unknown magic code: " + b0 + ", " + b1);
        }

        byte version = frame.readByte();
        // TODO  check version compatible here

        int fullLength = frame.readInt();
        short headLength = frame.readShort();
        byte messageType = frame.readByte();
        byte codecType = frame.readByte();
        byte compressor = frame.readByte();
        int requestId = frame.readInt();

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setCodec(codecType);
        rpcMessage.setId(requestId);
        rpcMessage.setCompressor(compressor);
        rpcMessage.setMessageType(messageType);

        if (headLength > 0) {
            byte[] bs = new byte[headLength];
            frame.readBytes(bs);
            Map<String, String> map = HeadMapSerializer.getInstance().decode(bs);
            rpcMessage.getHeadMap().putAll(map);
        }
        if (messageType == ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST) {
            rpcMessage.setBody(HeartbeatMessage.PING);
        } else if (messageType == ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE) {
            rpcMessage.setBody(HeartbeatMessage.PONG);
        } else {
            int bodyLength = fullLength - headLength - 13;
            if (bodyLength > 0) {
                byte[] bs = new byte[bodyLength];
                frame.readBytes(bs);
                Codec codec = CodecFactory.getCodec(codecType);
                rpcMessage.setBody(codec.decode(bs));
            }
        }

        return rpcMessage;
    }
}
