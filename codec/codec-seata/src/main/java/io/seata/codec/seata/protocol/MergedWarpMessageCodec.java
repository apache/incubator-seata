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

        int bufferSize = msgs.size() * 1024;
        ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
        byteBuffer.putShort((short)msgs.size());

        for (AbstractMessage msg : msgs) {

            short typeCode = msg.getTypeCode();
            byteBuffer.putShort(typeCode);

            MessageSeataCodec messageCodec = MessageCodecFactory.getMessageCodec(typeCode);
            messageCodec.encode(msg, byteBuffer);
        }

        byteBuffer.flip();
        int length = byteBuffer.limit();
        byte[] content = new byte[length + 4];
        intToBytes(length, content, 0);
        byteBuffer.get(content, 4, length);
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
