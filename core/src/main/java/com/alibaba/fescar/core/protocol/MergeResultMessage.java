/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.core.protocol;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Merge result message.
 *
 * @Author: jimin.jm @alibaba-inc.com
 * @Project: fescar -all
 * @DateTime: 2018 /10/10 14:55
 * @FileName: MergeResultMessage
 * @Description:
 */
public class MergeResultMessage extends AbstractMessage implements MergeMessage {
    private static final long serialVersionUID = -7719219648774528552L;
    /**
     * The Msgs.
     */
    public AbstractResultMessage[] msgs;
    private static final Logger LOGGER = LoggerFactory.getLogger(MergeResultMessage.class);

    /**
     * Get msgs abstract result message [ ].
     *
     * @return the abstract result message [ ]
     */
    public AbstractResultMessage[] getMsgs() {
        return msgs;
    }

    /**
     * Sets msgs.
     *
     * @param msgs the msgs
     */
    public void setMsgs(AbstractResultMessage[] msgs) {
        this.msgs = msgs;
    }

    @Override
    public short getTypeCode() {
        return TYPE_FESCAR_MERGE_RESULT;
    }

    @Override
    public byte[] encode() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(msgs.length * 1024);
        byteBuffer.putShort((short)msgs.length);
        for (AbstractMessage msg : msgs) {
            byte[] data = msg.encode();
            byteBuffer.putShort(msg.getTypeCode());
            byteBuffer.put(data);
        }

        byteBuffer.flip();
        int length = byteBuffer.limit();
        byte[] content = new byte[length + 4];
        intToBytes(length, content, 0);
        byteBuffer.get(content, 4, length);
        if (msgs.length > 20) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("msg in one fescar merge packet:" + msgs.length
                    + ",buffer size:" + content.length);
            }
        }
        return content;
    }

    @Override
    public boolean decode(ByteBuf in) {
        int i = in.readableBytes();
        if (i < 4) { return false; }

        i -= 4;
        int length = in.readInt();
        if (i < length) { return false; }
        byte[] buffer = new byte[length];
        in.readBytes(buffer);
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        decode(byteBuffer);
        return true;
    }

    /**
     * Decode.
     *
     * @param byteBuffer the byte buffer
     */
    public void decode(ByteBuffer byteBuffer) {
        short msgNum = byteBuffer.getShort();
        msgs = new AbstractResultMessage[msgNum];
        for (int idx = 0; idx < msgNum; idx++) {
            short typeCode = byteBuffer.getShort();
            MergedMessage message = getMergeResponseInstanceByCode(typeCode);
            message.decode(byteBuffer);
            msgs[idx] = (AbstractResultMessage)message;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MergeResultMessage ");
        for (AbstractMessage msg : msgs) { sb.append(msg.toString()).append("\n"); }
        return sb.toString();
    }
}
