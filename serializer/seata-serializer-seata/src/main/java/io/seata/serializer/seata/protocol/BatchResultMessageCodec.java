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
package io.seata.serializer.seata.protocol;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.BatchResultMessage;
import io.seata.serializer.seata.MessageCodecFactory;
import io.seata.serializer.seata.MessageSeataCodec;

/**
 * the type batch result message codec
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.5.0
 */
public class BatchResultMessageCodec extends AbstractMessageCodec {

    @Override
    public Class<?> getMessageClassType() {
        return BatchResultMessage.class;
    }

    @Override
    public <T> void encode(T t, ByteBuf out) {
        BatchResultMessage batchResultMessage = (BatchResultMessage) t;
        List<AbstractResultMessage> msgs = batchResultMessage.getResultMessages();
        List<Integer> msgIds = batchResultMessage.getMsgIds();

        final ByteBuf buffer = Unpooled.buffer(1024);
        buffer.writeInt(0); // write placeholder for content length

        buffer.writeShort((short) msgs.size());
        for (final AbstractMessage msg : msgs) {
            final ByteBuf subBuffer = Unpooled.buffer(1024);
            short typeCode = msg.getTypeCode();
            MessageSeataCodec messageCodec = MessageCodecFactory.getMessageCodec(typeCode);
            messageCodec.encode(msg, subBuffer);
            buffer.writeShort(msg.getTypeCode());
            buffer.writeBytes(subBuffer);
        }

        for (final Integer msgId : msgIds) {
            buffer.writeInt(msgId);
        }

        final int length = buffer.readableBytes();
        final byte[] content = new byte[length];
        buffer.setInt(0, length - 4);  // minus the placeholder length itself
        buffer.readBytes(content);

        if (msgs.size() > 20) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("msg in one packet:" + msgs.size() + ",buffer size:" + content.length);
            }
        }
        out.writeBytes(content);

    }

    @Override
    public <T> void decode(T t, ByteBuffer in) {
        BatchResultMessage batchResultMessage = (BatchResultMessage) t;

        if (in.remaining() < 4) {
            return;
        }
        int length = in.getInt();
        if (in.remaining() < length) {
            return;
        }
        byte[] buffer = new byte[length];
        in.get(buffer);
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        decode(batchResultMessage, byteBuffer);
    }

    /**
     * Decode.
     *
     * @param batchResultMessage the batch result message
     * @param byteBuffer         the byte buffer
     */
    protected void decode(BatchResultMessage batchResultMessage, ByteBuffer byteBuffer) {
        short msgNum = byteBuffer.getShort();
        List<AbstractResultMessage> msgs = new ArrayList<>();
        List<Integer> msgIds = new ArrayList<>();
        for (int idx = 0; idx < msgNum; idx++) {
            short typeCode = byteBuffer.getShort();
            AbstractMessage abstractResultMessage = MessageCodecFactory.getMessage(typeCode);
            MessageSeataCodec messageCodec = MessageCodecFactory.getMessageCodec(typeCode);
            messageCodec.decode(abstractResultMessage, byteBuffer);
            msgs.add((AbstractResultMessage) abstractResultMessage);
        }

        for (int idx = 0; idx < msgNum; idx++) {
            msgIds.add(byteBuffer.getInt());
        }

        batchResultMessage.setResultMessages(msgs);
        batchResultMessage.setMsgIds(msgIds);

    }
}
