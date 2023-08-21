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
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.core.protocol.HeartbeatMessage;

import io.seata.core.protocol.ProtocolConstants;
import io.seata.core.rpc.RegisterCheckAuthHandler;
import io.seata.core.rpc.netty.ProtocolDecoder;
import io.seata.core.serializer.Serializer;
import io.seata.core.serializer.SerializerServiceLoader;
import io.seata.core.serializer.SerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.ServiceLoader;

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
 * @author Bughue
 * @see ProtocolV0Encoder
 * @since 2.0.0
 */
public class ProtocolV0Decoder implements ProtocolDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolV0Decoder.class);

    private static final SerializerV0 serializer  =  EnhancedServiceLoader.load(SerializerV0.class);;

    @Override
    public ProtocolV0RpcMessage decodeFrame(ByteBuf in) {
        ProtocolV0RpcMessage rpcMessage = new ProtocolV0RpcMessage();
        // todo 旧版本是直接返回跳过了，我们需要保留这个逻辑？【特殊】
        if (in.readableBytes() < ProtocolV0Constants.HEAD_LENGTH) {
            throw new IllegalArgumentException("Nothing to decode.");
        }

        // todo 这里是为了bodyLength不满足要求时reset，意义是什么？【特殊】
        in.markReaderIndex();

        // todo 外层已经判断过了，这里可以跳过
        short protocol = in.readShort();

        int flag = (int) in.readShort();

        boolean isHeartbeat = (ProtocolV0Constants.FLAG_HEARTBEAT & flag) > 0;
        boolean isRequest = (ProtocolV0Constants.FLAG_REQUEST & flag) > 0;
        boolean isSeataCodec = (ProtocolV0Constants.FLAG_SEATA_CODEC & flag) > 0;
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

            return rpcMessage;
        }

        if (bodyLength > 0 && in.readableBytes() < bodyLength) {
            // todo 【特殊】
            in.resetReaderIndex();
            throw new IllegalArgumentException("readableBytes < bodyLength");
        }

        rpcMessage.setAsync((ProtocolV0Constants.FLAG_ASYNC & flag) > 0);
        rpcMessage.setHeartbeat(false);
        rpcMessage.setRequest(isRequest);

        try {
            // v0_1
            // todo serializer==null
//            MessageCodecV0 msgCodec = serializer.getMsgInstanceByCode(typeCode);
//            if (!msgCodec.decode(in)) {
//                throw new IllegalArgumentException("decode fail.");
//            }
//            rpcMessage.setBody(msgCodec);

            // v0_2
            int length = in.readableBytes();
            byte[] bs = new byte[length];
            in.readBytes(bs);
            byte[] bs2 = new byte[2 + length];
            bs2[0] = (byte) (0x00FF & (typeCode>>8));
            bs2[1] = (byte) (0x00FF & typeCode);
            System.arraycopy(bs,0,bs2,2, length);
            byte codecType = isSeataCodec? SerializerType.SEATA.getCode():SerializerType.HESSIAN.getCode();
            Serializer serializer = SerializerServiceLoader.load(SerializerType.getByCode(codecType), ProtocolConstants.VERSION_0);
            rpcMessage.setBody(serializer.deserialize(bs2));

        } catch (Exception e) {
            LOGGER.error("decode error", "", e);
            throw e;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Receive:" + rpcMessage.getBody() + ",messageId:"
                    + msgId);
        }
        return rpcMessage;
    }


}
