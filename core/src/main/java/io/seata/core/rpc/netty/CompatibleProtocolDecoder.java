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
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.seata.core.exception.DecodeException;
import io.seata.core.protocol.ProtocolConstants;
import io.seata.core.rpc.netty.v0.ProtocolV0Decoder;
import io.seata.core.rpc.netty.v1.ProtocolV1Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * <pre>
 * (> 0.7.0)
 * 0     1     2     3     4     5     6     7     8     9    10     11    12    13    14    15    16
 * +-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+
 * |   magic   |Proto|     Full length       |    Head   | Msg |Seria|Compr|     RequestId         |
 * |   code    |colVer|    (head+body)      |   Length  |Type |lizer|ess  |                       |
 * +-----------+-----------+-----------+-----------+-----------+-----------+-----------+-----------+
 *
 * (<= 0.7.0)
 * 0     1     2     3     4           6           8          10           12          14
 * +-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+
 * |   0xdada  |   flag    | typecode/ |                 requestid                     |
 * |           |           | bodylength|                                               |
 * +-----------+-----------+-----------+-----------+-----------+-----------+-----------+
 *
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
public class CompatibleProtocolDecoder extends LengthFieldBasedFrameDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompatibleProtocolDecoder.class);
    private static Map<Byte, ProtocolDecoder> protocolDecoderMap;

    public CompatibleProtocolDecoder() {
        // default is 8M
        this(ProtocolConstants.MAX_FRAME_LENGTH);
        protocolDecoderMap = ImmutableMap.<Byte, ProtocolDecoder>builder()
                .put(ProtocolConstants.VERSION_0, new ProtocolV0Decoder())
                .put(ProtocolConstants.VERSION_1, new ProtocolV1Decoder())
                .build();
    }

    public CompatibleProtocolDecoder(int maxFrameLength) {
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
        Object decoded;
        try {
            decoded = super.decode(ctx, in);
            if (decoded instanceof ByteBuf) {
                ByteBuf frame = (ByteBuf) decoded;
                try {
                    byte version = decideVersion(frame);
                    ProtocolDecoder decoder = protocolDecoderMap.get(version);
                    if (decoder == null) {
                        // todo 要不要适配当前版本？
                        throw new IllegalArgumentException("Unknown version: " + version);
                    }
                    return decoder.decodeFrame(frame);
                } finally {
                    frame.release();
                }
            }
        } catch (Exception exx) {
            LOGGER.error("Decode frame error, cause: {}", exx.getMessage());
            throw new DecodeException(exx);
        }
        return decoded;
    }

    protected byte decideVersion(ByteBuf frame) {
        frame.markReaderIndex();
        byte b0 = frame.readByte();
        byte b1 = frame.readByte();
        if (ProtocolConstants.MAGIC_CODE_BYTES[0] != b0
                || ProtocolConstants.MAGIC_CODE_BYTES[1] != b1) {
            throw new IllegalArgumentException("Unknown magic code: " + b0 + ", " + b1);
        }

        byte version = frame.readByte();
        frame.resetReaderIndex();
        return version;
    }


}
