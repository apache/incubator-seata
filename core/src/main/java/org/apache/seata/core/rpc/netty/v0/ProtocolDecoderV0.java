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
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.seata.core.exception.DecodeException;
import org.apache.seata.core.protocol.HeartbeatMessage;

import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.rpc.netty.ProtocolDecoder;
import org.apache.seata.core.serializer.Serializer;
import org.apache.seata.core.serializer.SerializerServiceLoader;
import org.apache.seata.core.serializer.SerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @see ProtocolEncoderV0
 */
public class ProtocolDecoderV0 extends LengthFieldBasedFrameDecoder implements ProtocolDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolDecoderV0.class);

    public ProtocolDecoderV0() {
        /*
        int maxFrameLength,
        int lengthFieldOffset,  magic code is 2B, and version is 1B, and then FullLength. so value is 3
        int lengthFieldLength,  FullLength is int(4B). so values is 4
        int lengthAdjustment,   FullLength include all data and read 7 bytes before, so the left length is (FullLength-7). so values is -7
        int initialBytesToStrip we will check magic code and version self, so do not strip any bytes. so values is 0
        */
        super(ProtocolConstants.MAX_FRAME_LENGTH, 3, 4, -7, 0);
    }

    @Override
    public RpcMessage decodeFrame(ByteBuf in) {
        ProtocolRpcMessageV0 rpcMessage = new ProtocolRpcMessageV0();
        if (in.readableBytes() < ProtocolConstantsV0.HEAD_LENGTH) {
            throw new IllegalArgumentException("Nothing to decode.");
        }

        in.markReaderIndex();
        short protocol = in.readShort();
        int flag = (int) in.readShort();

        boolean isHeartbeat = (ProtocolConstantsV0.FLAG_HEARTBEAT & flag) > 0;
        boolean isRequest = (ProtocolConstantsV0.FLAG_REQUEST & flag) > 0;
        boolean isSeataCodec = (ProtocolConstantsV0.FLAG_SEATA_CODEC & flag) > 0;
        rpcMessage.setSeataCodec(isSeataCodec);

        short bodyLength = 0;
        short typeCode = 0;
        if (!isSeataCodec) {
            bodyLength = in.readShort();
        } else {
            typeCode = in.readShort();
        }
        long msgId = in.readLong();
        rpcMessage.setId(msgId);
        if (isHeartbeat) {
            rpcMessage.setAsync(true);
            rpcMessage.setHeartbeat(isHeartbeat);
            rpcMessage.setRequest(isRequest);
            if (isRequest) {
                rpcMessage.setBody(HeartbeatMessage.PING);
            } else {
                rpcMessage.setBody(HeartbeatMessage.PONG);
            }

            return rpcMessage.protocolMsg2RpcMsg();
        }

        if (bodyLength > 0 && in.readableBytes() < bodyLength) {
            in.resetReaderIndex();
            throw new IllegalArgumentException("readableBytes < bodyLength");
        }

        rpcMessage.setAsync((ProtocolConstantsV0.FLAG_ASYNC & flag) > 0);
        rpcMessage.setHeartbeat(false);
        rpcMessage.setRequest(isRequest);

        try {
            int length = in.readableBytes();
            byte[] bs = new byte[length];
            in.readBytes(bs);

            // fill messageType in v0
            byte[] bs2 = new byte[2 + length];
            bs2[0] = (byte) (0x00FF & (typeCode >> 8));
            bs2[1] = (byte) (0x00FF & typeCode);
            System.arraycopy(bs, 0, bs2, 2, length);
            byte codecType = isSeataCodec ? SerializerType.SEATA.getCode() : SerializerType.HESSIAN.getCode();
            Serializer serializer = SerializerServiceLoader.load(SerializerType.getByCode(codecType), ProtocolConstants.VERSION_0);
            rpcMessage.setBody(serializer.deserialize(bs2));
        } catch (Exception e) {
            LOGGER.error("decode error", e);
            throw e;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Receive:" + rpcMessage.getBody() + ", messageId:" + msgId);
        }
        return rpcMessage.protocolMsg2RpcMsg();
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        try {
            return decodeFrame(in);
        } catch (Exception exx) {
            LOGGER.error("Decode frame error, cause: {}", exx.getMessage());
            throw new DecodeException(exx);
        }
    }
}
