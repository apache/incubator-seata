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
package io.seata.codec.seata.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.seata.codec.seata.MessageCodecFactory;
import io.seata.codec.seata.MessageSeataCodec;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.MergedWarpMessage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


/**
 * The type Merged warp message codec.
 *
 * @author zhangsen
 */
public class MergedWarpMessageCodec extends AbstractMessageCodec {

    @Override
    public Class<?> getMessageClassType() {
        return MergedWarpMessage.class;
    }

    @Override
    public <T> void encode(T t, ByteBuffer out){
        MergedWarpMessage mergedWarpMessage = (MergedWarpMessage) t;
        List<AbstractMessage> msgs = mergedWarpMessage.msgs;

        final ByteBuf buffer = Unpooled.buffer(1024);
        buffer.writeInt(0); // write placeholder for content length

        buffer.writeShort((short) msgs.size());
        for (final AbstractMessage msg : msgs) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            short typeCode = msg.getTypeCode();
            MessageSeataCodec messageCodec = MessageCodecFactory.getMessageCodec(typeCode);
            messageCodec.encode(msg, byteBuffer);
            byteBuffer.flip();
            buffer.writeShort(msg.getTypeCode());
            buffer.writeBytes(byteBuffer);
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
        out.put(content);
    }

    @Override
    public <T> void decode(T t, ByteBuffer in) {
        MergedWarpMessage mergedWarpMessage = (MergedWarpMessage) t;

        if (in.remaining() < 4) {
            return ;
        }
        int length = in.getInt();
        if (in.remaining() < length) {
            return ;
        }
        byte[] buffer = new byte[length];
        in.get(buffer);
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        doDecode(mergedWarpMessage, byteBuffer);
    }

    private void doDecode(MergedWarpMessage mergedWarpMessage, ByteBuffer byteBuffer) {
        short msgNum = byteBuffer.getShort();
        List<AbstractMessage> msgs = new ArrayList<AbstractMessage>();
        for (int idx = 0; idx < msgNum; idx++) {
            short typeCode = byteBuffer.getShort();
            AbstractMessage abstractMessage = MessageCodecFactory.getMessage(typeCode);
            MessageSeataCodec messageCodec = MessageCodecFactory.getMessageCodec(typeCode);
            messageCodec.decode(abstractMessage, byteBuffer);
            msgs.add(abstractMessage);
        }
        mergedWarpMessage.msgs = msgs;
    }

}
