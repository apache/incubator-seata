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
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.seata.core.codec.Codec;
import io.seata.core.codec.CodecFactory;
import io.seata.core.compressor.Compressor;
import io.seata.core.compressor.CompressorFactory;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.ProtocolConstants;
import io.seata.core.protocol.RpcMessage;
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
 * @see ProtocolV1Encoder
 * @since 0.7.0
 */
public class ProtocolV1Decoder extends LengthFieldBasedFrameDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolV1Decoder.class);

    public ProtocolV1Decoder() {
        // default is 8M
        this(ProtocolConstants.MAX_FRAME_LENGTH);
    }

    public ProtocolV1Decoder(int maxFrameLength) {
        /*
        int maxFrameLength,      
        int lengthFieldOffset,  magic code is 2B, and version is 1B, and then FullLength. so value is 3
        int lengthFieldLength,  FullLength is int(4B). so values is 4
        int lengthAdjustment,   FullLength include all data and read 7 bytes before, so the left length is (FullLength-7). so values is -7
        int initialBytesToStrip we will check magic code and version self, so do not strip any bytes. so values is 0
        */
        super(maxFrameLength, 3, 4, -7, 0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        Object decoded = super.decode(ctx, in);
        if (decoded instanceof ByteBuf) {
            ByteBuf frame = (ByteBuf) decoded;
            try {
                return decodeFrame(frame);
            } catch (Exception e) {
                LOGGER.error("Decode frame error!", e);
                throw e;
            } finally {
                frame.release();
            }
        }
        return decoded;
    }

    public Object decodeFrame(ByteBuf frame) {
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
        byte compressorType = frame.readByte();
        int requestId = frame.readInt();

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setCodec(codecType);
        rpcMessage.setId(requestId);
        rpcMessage.setCompressor(compressorType);
        rpcMessage.setMessageType(messageType);

        // direct read head with zero-copy
        int headMapLength = headLength - ProtocolConstants.V1_HEAD_LENGTH;
        if (headMapLength > 0) {
            Map<String, String> map = HeadMapSerializer.getInstance().decode(frame, headMapLength);
            rpcMessage.getHeadMap().putAll(map);
        }

        // read body
        if (messageType == ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST) {
            rpcMessage.setBody(HeartbeatMessage.PING);
        } else if (messageType == ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE) {
            rpcMessage.setBody(HeartbeatMessage.PONG);
        } else {
            int bodyLength = fullLength - headLength;
            if (bodyLength > 0) {
                byte[] bs = new byte[bodyLength];
                frame.readBytes(bs);
                Compressor compressor = CompressorFactory.getCompressor(compressorType);
                bs = compressor.decompress(bs);
                Codec codec = CodecFactory.getCodec(codecType);
                rpcMessage.setBody(codec.decode(bs));
            }
        }

        return rpcMessage;
    }
}
