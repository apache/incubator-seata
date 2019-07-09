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
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.codec.Codec;
import io.seata.core.codec.CodecFactory;
import io.seata.core.codec.CodecType;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.protocol.HeartbeatMessage;
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
    private static int HEAD_LENGTH = 16;
    private static final short FLAG_REQUEST = 0x80;
    private static final short FLAG_ASYNC = 0x40;
    private static final short FLAG_HEARTBEAT = 0x20;
    private static final short FLAG_SEATA_CODEC = 0x10;

    private static Configuration configuration = ConfigurationFactory.getInstance();

    private static String serialize = configuration.getConfig(ConfigurationKeys.SERIALIZE_FOR_RPC,
        CodecType.SEATA.name());

    /**
     * The constant UTF8.
     */
    protected static final Charset UTF8 = StandardCharsets.UTF_8;

    /**
     * encode the msg: magic, flag, msgId, bodyLength, body
     *
     * @param ctx
     * @param msg
     * @param out
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage msg, ByteBuf out) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate(128);

        //codec
        CodecType codecType = CodecType.getByCode(this.getSerializer());
        byte codecCode = codecType.getCode();

        //header, the flag: 0000 0000 flags(4) codec(4)
        byteBuffer.putShort(MAGIC);
        short flag = (short)((msg.isAsync() ? FLAG_ASYNC : 0)
            | (msg.isHeartbeat() ? FLAG_HEARTBEAT : 0)
            | (msg.isRequest() ? FLAG_REQUEST : 0)
            //                | (msgCodec != null ? FLAG_SEATA_CODEC : 0)
            | FLAG_SEATA_CODEC
            | codecCode);

        byteBuffer.putShort(flag);

        //heart beat
        if (msg.isHeartbeat()) {
            byteBuffer.putLong(msg.getId());
            //body length
            byteBuffer.putInt(0);
            byteBuffer.flip();
            byte[] content = new byte[byteBuffer.limit()];
            byteBuffer.get(content);
            out.writeBytes(content);
            return;
        }

        //msgId before body
        byteBuffer.putLong(msg.getId());

        //the body
        try {
            //codec
            Codec codec = CodecFactory.getCodec(codecCode);
            //get body bytes
            byte[] bodyBytes = codec.encode(msg.getBody());
            int bodyBytesLength = bodyBytes.length;
            ByteBuffer contentByteBuffer = ByteBuffer.allocate(bodyBytesLength + 20);

            byteBuffer.flip();
            contentByteBuffer.put(byteBuffer);
            contentByteBuffer.putInt(bodyBytesLength);
            contentByteBuffer.put(bodyBytes);
            contentByteBuffer.flip();

            //write content
            byte[] content = new byte[contentByteBuffer.limit()];
            contentByteBuffer.get(content);
            out.writeBytes(content);
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
        //      the msg: magic, flag, msgId, bodyLength, body

        //header length: 16
        if (in.readableBytes() < HEAD_LENGTH) {
            return;
        }
        in.markReaderIndex();
        //magic
        short protocol = in.readShort();
        if (protocol != MAGIC) {
            String emsg = "decode error,Unknown protocol: " + protocol + ",will close channel:" + ctx.channel();
            LOGGER.error(emsg);
            ctx.channel().close();
            return;
        }
        //flag
        short flag = in.readShort();

        boolean isHeartbeat = (FLAG_HEARTBEAT & flag) > 0;
        boolean isRequest = (FLAG_REQUEST & flag) > 0;
        CodecType codecType = CodecType.getByCode(flag & 0x0F);

        //msgId
        long msgId = in.readLong();

        //heart beat msg
        if (isHeartbeat) {
            //read length=0
            in.readInt();
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

        //bodyLength
        int bodyLength = in.readInt();

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
            //codec
            Codec codec = CodecFactory.getCodec(codecType.getCode());
            byte[] bodyBytes = new byte[bodyLength];
            in.readBytes(bodyBytes);
            rpcMessage.setBody(codec.decode(bodyBytes));

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

    protected String getSerializer() {
        return serialize;
    }
}
