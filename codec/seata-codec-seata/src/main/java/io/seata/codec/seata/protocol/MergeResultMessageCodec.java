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
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.MergeResultMessage;

import java.nio.ByteBuffer;

/**
 * The type Merge result message codec.
 *
 * @author zhangsen
 */
public class MergeResultMessageCodec extends AbstractMessageCodec {

    @Override
    public Class<?> getMessageClassType() {
        return MergeResultMessage.class;
    }

    @Override
    public <T> void encode(T t, ByteBuffer out){
        MergeResultMessage mergeResultMessage = (MergeResultMessage) t;
        AbstractResultMessage[] msgs = mergeResultMessage.getMsgs();

        ByteBuffer byteBuffer = ByteBuffer.allocate(msgs.length * 1024);
        byteBuffer.putShort((short)msgs.length);
        for (AbstractMessage msg : msgs) {
            //get messageCodec
            short typeCode = msg.getTypeCode();
            //put typeCode
            byteBuffer.putShort(typeCode);
            MessageSeataCodec messageCodec = MessageCodecFactory.getMessageCodec(typeCode);
            messageCodec.encode(msg, byteBuffer);
        }

        byteBuffer.flip();
        int length = byteBuffer.limit();
        byte[] content = new byte[length + 4];
        intToBytes(length, content, 0);
        byteBuffer.get(content, 4, length);
        if (msgs.length > 20) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("msg in one services merge packet:" + msgs.length  + ",buffer size:" + content.length);
            }
        }
        out.put(content);
    }

    @Override
    public <T> void decode(T t, ByteBuffer in) {
        MergeResultMessage mergeResultMessage = (MergeResultMessage) t;

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
        decode(mergeResultMessage, byteBuffer);
    }

    /**
     * Decode.
     *
     * @param mergeResultMessage the merge result message
     * @param byteBuffer         the byte buffer
     */
    protected void decode(MergeResultMessage mergeResultMessage, ByteBuffer byteBuffer) {
        //msgs size
        short msgNum = byteBuffer.getShort();
        AbstractResultMessage[] msgs = new AbstractResultMessage[msgNum];
        for (int idx = 0; idx < msgNum; idx++) {
            short typeCode = byteBuffer.getShort();
            AbstractMessage abstractResultMessage = MessageCodecFactory.getMessage(typeCode);
            MessageSeataCodec messageCodec = MessageCodecFactory.getMessageCodec(typeCode);
            messageCodec.decode(abstractResultMessage, byteBuffer);
            msgs[idx] = (AbstractResultMessage)abstractResultMessage;
        }
        mergeResultMessage.setMsgs(msgs);
    }

}
