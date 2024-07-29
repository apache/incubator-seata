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
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.seata.core.exception.DecodeException;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.rpc.netty.v0.ProtocolDecoderV0;
import org.apache.seata.core.rpc.netty.v0.ProtocolEncoderV0;
import org.apache.seata.core.rpc.netty.v1.ProtocolDecoderV1;
import org.apache.seata.core.rpc.netty.v1.ProtocolEncoderV1;
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
 */
public class MultiProtocolDecoder extends LengthFieldBasedFrameDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiProtocolDecoder.class);
    private final Map<Byte, ProtocolDecoder> protocolDecoderMap;

    private final Map<Byte, ProtocolEncoder> protocolEncoderMap;
    
    private final ChannelHandler[] channelHandlers;

    public MultiProtocolDecoder(ChannelHandler... channelHandlers) {
        // default is 8M
        this(ProtocolConstants.MAX_FRAME_LENGTH, channelHandlers);
    }

    public MultiProtocolDecoder() {
        // default is 8M
        this(ProtocolConstants.MAX_FRAME_LENGTH, null);
    }

    public MultiProtocolDecoder(int maxFrameLength, ChannelHandler[] channelHandlers) {
        /*
        int maxFrameLength,      
        int lengthFieldOffset,  magic code is 2B, and version is 1B, and then FullLength. so value is 3
        int lengthFieldLength,  FullLength is int(4B). so values is 4
        int lengthAdjustment,   FullLength include all data and read 7 bytes before, so the left length is (FullLength-7). so values is -7
        int initialBytesToStrip we will check magic code and version self, so do not strip any bytes. so values is 0
        */
        super(maxFrameLength, 3, 4, -7, 0);
        this.protocolDecoderMap =
            ImmutableMap.<Byte, ProtocolDecoder>builder().put(ProtocolConstants.VERSION_0, new ProtocolDecoderV0())
                .put(ProtocolConstants.VERSION_1, new ProtocolDecoderV1()).build();
        this.protocolEncoderMap =
            ImmutableMap.<Byte, ProtocolEncoder>builder().put(ProtocolConstants.VERSION_0, new ProtocolEncoderV0())
                .put(ProtocolConstants.VERSION_1, new ProtocolEncoderV1()).build();
        this.channelHandlers = channelHandlers;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame;
        Object decoded;
        byte version;
        try {
            if (isV0(in)) {
                decoded = in;
                version = ProtocolConstants.VERSION_0;
            } else {
                decoded = super.decode(ctx, in);
                version = decideVersion(decoded);
            }

            if (decoded instanceof ByteBuf) {
                frame = (ByteBuf) decoded;
                ProtocolDecoder decoder = protocolDecoderMap.get(version);
                ProtocolEncoder encoder = protocolEncoderMap.get(version);
                try {
                    if (decoder == null || encoder == null) {
                        throw new UnsupportedOperationException("Unsupported version: " + version);
                    }
                    return decoder.decodeFrame(frame);
                } finally {
                    if (version != ProtocolConstants.VERSION_0) {
                        frame.release();
                    }
                    ctx.pipeline().addLast((ChannelHandler)decoder);
                    ctx.pipeline().addLast((ChannelHandler)encoder);
                    if (channelHandlers != null) {
                        ctx.pipeline().addLast(channelHandlers);
                    }
                    ctx.pipeline().remove(this);
                }
            }
        } catch (Exception exx) {
            LOGGER.error("Decode frame error, cause: {}", exx.getMessage());
            throw new DecodeException(exx);
        }
        return decoded;
    }

    protected byte decideVersion(Object in) {
        if (in instanceof ByteBuf) {
            ByteBuf frame = (ByteBuf) in;
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
        return -1;
    }


    protected boolean isV0(ByteBuf in) {
        boolean isV0 = false;
        in.markReaderIndex();
        byte b0 = in.readByte();
        byte b1 = in.readByte();
        // v1/v2/v3 : b2 = version
        // v0 : 1st byte in FLAG(2byte:0x10/0x20/0x40/0x80)
        byte b2 = in.readByte();
        if (ProtocolConstants.MAGIC_CODE_BYTES[0] == b0
                && ProtocolConstants.MAGIC_CODE_BYTES[1] == b1
                && 0 == b2) {
            isV0 = true;
        }

        in.resetReaderIndex();
        return isV0;
    }

    protected boolean isV0(byte version) {
        return version == ProtocolConstants.VERSION_0;
    }
}