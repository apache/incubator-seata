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
import java.nio.charset.Charset;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.codec.CodecFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.MessageCodec;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.convertor.PbConvertor;
import io.seata.core.protocol.protobuf.HeartbeatMessageProto;
import io.seata.core.codec.CodecType;
import io.seata.core.protocol.serialize.ProtobufConvertManager;
import io.seata.core.protocol.serialize.ProtobufSerialzer;
import io.seata.core.codec.CodecType;
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

    private static Configuration configuration = ConfigurationFactory.getInstance();

    private static String serialize = configuration.getConfig(ConfigurationKeys.SERIALIZE_FOR_RPC);

    /**
     * The constant UTF8.
     */
    protected static final Charset UTF8 = Charset.forName("utf-8");

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage msg, ByteBuf out) throws Exception {
        if (CodecType.PROTOBUF.name().equalsIgnoreCase(serialize)) {
            //translate
            Object body = msg.getBody();
            final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(
                body.getClass().getName());
            Object newBody = pbConvertor.convert2Proto(body);
            msg.setBody(newBody);
        }

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

        if (msg.getBody() instanceof HeartbeatMessage || msg.getBody() instanceof HeartbeatMessageProto) {
            byteBuffer.putShort((short)0);
            byteBuffer.putLong(msg.getId());
            byteBuffer.flip();
            byte[] content = new byte[byteBuffer.limit()];
            byteBuffer.get(content);
            out.writeBytes(content);
            return;
        }

        try {
            if (null != msgCodec) {
                byteBuffer.putShort(msgCodec.getTypeCode());
                byteBuffer.putLong(msg.getId());

                byteBuffer.flip();
                byte[] content = new byte[byteBuffer.limit()];
                byteBuffer.get(content);
                out.writeBytes(content);
                out.writeBytes(msgCodec.encode());
            } else {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("msg:" + msg.getBody().toString());
                }
                byte[] body = CodecFactory.encode(CodecType.PROTOBUF.getCode(),msg.getBody());
                final String name = msg.getBody().getClass().getName();
                final short bodyLength = (short)(body.length + name.length() + 4);
                byteBuffer.putShort(bodyLength);
                byteBuffer.putLong(msg.getId());
                final byte[] nameBytes = name.getBytes(UTF8);
                byteBuffer.putInt(nameBytes.length);
                byteBuffer.put(nameBytes);
                byteBuffer.put(body);

                byteBuffer.flip();
                byte[] content = new byte[byteBuffer.limit()];
                byteBuffer.get(content);
                out.writeBytes(content);
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
            LOGGER.error("decode less than header length");
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
                int clazzNameLength = in.readInt();
                byte[] clazzName = new byte[clazzNameLength];
                in.readBytes(clazzName);
                byte[] body = new byte[bodyLength - clazzNameLength - 4];
                in.readBytes(body);
                final String clazz = new String(clazzName, UTF8);
                Object bodyObject = CodecFactory.decode(CodecType.PROTOBUF.getCode(),clazz, body);

                if (CodecType.PROTOBUF.name().equalsIgnoreCase(serialize)) {
                    final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchReversedConvertor(clazz);
                    Object newBody = pbConvertor.convert2Model(bodyObject);
                    rpcMessage.setBody(newBody);
                } else {
                    rpcMessage.setBody(bodyObject);
                }
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
}
