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

import java.nio.ByteBuffer;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.MessageCodec;
import io.seata.core.protocol.RpcMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Message codec handler.
 * RpcMessage protocol
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
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /9/14
 */
public class MessageCodecHandler extends ByteToMessageCodec<RpcMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageCodecHandler.class);
    private static short MAGIC = (short)0xdada;
    private static int HEAD_LENGTH = 14;
    private static final int FLAG_REQUEST = 0x80;
    private static final int FLAG_ASYNC = 0x40;
    private static final int FLAG_HEARTBEAT = 0x20;
    private static final int FLAG_SEATA_CODEC = 0x10;
    private static final int MAGIC_HALF = -38;
    private static final int NOT_FOUND_INDEX = -1;

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage msg, ByteBuf out) throws Exception {
        MessageCodec msgCodec = null;
        ByteBuffer byteBuffer = ByteBuffer.allocate(128);
        if (msg.getBody() instanceof MessageCodec) {
            msgCodec = (MessageCodec)msg.getBody();
        }
        byteBuffer.putShort(MAGIC);
        int flag = (msg.isAsync() ? FLAG_ASYNC : 0)
            | (msg.isHeartbeat() ? FLAG_HEARTBEAT : 0)
            | (msg.isRequest() ? FLAG_REQUEST : 0)
            | (msgCodec != null ? FLAG_SEATA_CODEC : 0);

        byteBuffer.putShort((short)flag);

        if (msg.getBody() instanceof HeartbeatMessage) {
            byteBuffer.putShort((short)0);
            byteBuffer.putLong(msg.getId());
            byteBuffer.flip();
            out.writeBytes(byteBuffer);
            return;
        }

        try {
            if (null != msgCodec) {
                byteBuffer.putShort(msgCodec.getTypeCode());
                byteBuffer.putLong(msg.getId());

                byteBuffer.flip();
                out.writeBytes(byteBuffer);
                out.writeBytes(msgCodec.encode());
            } else {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("msg:" + msg.getBody().toString());
                }
                byte[] body = hessianSerialize(msg.getBody());
                byteBuffer.putShort((short)body.length);
                byteBuffer.putLong(msg.getId());
                byteBuffer.put(body);

                byteBuffer.flip();
                out.writeBytes(byteBuffer);
            }
        } catch (Exception e) {
            LOGGER.error(msg.getBody() + " encode error", "", e);
            throw e;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Send:" + msg.getBody());
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        if (in.readableBytes() < HEAD_LENGTH) {
            return;
        }
        in.markReaderIndex();
        short protocol = in.readShort();
        if (protocol != MAGIC) {
            String emsg = "decode error,Unknown protocol: " + protocol + ",will close channel:" + ctx.channel();
            LOGGER.error(emsg);
            ctx.channel().close();
            return;
        }

        int flag = (int)in.readShort();

        boolean isHeartbeat = (FLAG_HEARTBEAT & flag) > 0;
        boolean isRequest = (FLAG_REQUEST & flag) > 0;
        boolean isSeataCodec = (FLAG_SEATA_CODEC & flag) > 0;

        short bodyLength = 0;
        short typeCode = 0;
        if (!isSeataCodec) { bodyLength = in.readShort(); } else { typeCode = in.readShort(); }
        long msgId = in.readLong();
        if (isHeartbeat) {
            RpcMessage rpcMessage = new RpcMessage();
            rpcMessage.setId(msgId);
            rpcMessage.setAsync(true);
            rpcMessage.setHeartbeat(isHeartbeat);
            rpcMessage.setRequest(isRequest);
            if (isRequest) {
                rpcMessage.setBody(HeartbeatMessage.PING);
            } else {
                rpcMessage.setBody(HeartbeatMessage.PONG);
            }
            out.add(rpcMessage);
            return;
        }

        if (bodyLength > 0 && in.readableBytes() < bodyLength) {
            in.resetReaderIndex();
            return;
        }

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(msgId);
        rpcMessage.setAsync((FLAG_ASYNC & flag) > 0);
        rpcMessage.setHeartbeat(false);
        rpcMessage.setRequest(isRequest);

        try {
            if (isSeataCodec) {
                MessageCodec msgCodec = AbstractMessage.getMsgInstanceByCode(typeCode);
                if (!msgCodec.decode(in)) {
                    in.resetReaderIndex();
                    return;
                }
                rpcMessage.setBody(msgCodec);
            } else {
                byte[] body = new byte[bodyLength];
                in.readBytes(body);
                Object bodyObject = hessianDeserialize(body);
                rpcMessage.setBody(bodyObject);
            }
        } catch (Exception e) {
            LOGGER.error("decode error", "", e);
            throw e;
        }
        out.add(rpcMessage);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Receive:" + rpcMessage.getBody() + ",messageId:"
                + msgId);
        }

    }

    /**
     * Hessian serialize byte [ ].
     *
     * @param object the object
     * @return the byte [ ]
     * @throws Exception the exception
     */
    private static byte[] hessianSerialize(Object object) throws Exception {
        if (object == null) {
            throw new NullPointerException();
        }
        //todo user defined exx
        throw new RuntimeException("hessianSerialize not support");

    }

    /**
     * Hessian deserialize object.
     *
     * @param bytes the bytes
     * @return the object
     * @throws Exception the exception
     */
    private static Object hessianDeserialize(byte[] bytes) throws Exception {
        if (bytes == null) {
            throw new NullPointerException();
        }
        //todo user defined exx
        throw new RuntimeException("hessianDeserialize not support");

    }
}
