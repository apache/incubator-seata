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

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Merged warp message.
 *
 * @Author: jimin.jm @alibaba-inc.com
 * @Project: fescar -all
 * @DateTime: 2018 /10/9 16:55
 * @FileName: MergedWarpMessage
 * @Description:
 */
public class MergedWarpMessage extends AbstractMessage implements Serializable,MergeMessage {
    private static final long serialVersionUID = -5758802337446717090L;
    /**
     * The Msgs.
     */
    public List<AbstractMessage> msgs = new ArrayList<AbstractMessage>();
    /**
     * The Msg ids.
     */
    public List<Long> msgIds = new ArrayList<Long>();
    private static final Logger LOGGER = LoggerFactory.getLogger(MergedWarpMessage.class);

    @Override
    public short getTypeCode() {
        return TYPE_FESCAR_MERGE;
    }

    @Override
    public byte[] encode() {
        int bufferSize = msgs.size() * 1024;
        ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
        byteBuffer.putShort((short) msgs.size());
        for (AbstractMessage msg : msgs) {
            //msg.setChannelHandlerContext(ctx);
            byte[] data = msg.encode();
            byteBuffer.putShort(msg.getTypeCode());
            byteBuffer.put(data);
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
        return content;
    }

    @Override
    public boolean decode(ByteBuf in) {
        int i = in.readableBytes();
        if (i < 4) {
            return false;
        }

        i -= 4;
        int length = in.readInt();
        if (i < length) {
            return false;
        }

        byte[] buffer = new byte[length];
        in.readBytes(buffer);
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        doDecode(byteBuffer);
        return true;
    }

    private void doDecode(ByteBuffer byteBuffer) {
        short msgNum = byteBuffer.getShort();
        for (int idx = 0; idx < msgNum; idx++) {
            short typeCode = byteBuffer.getShort();
            MergedMessage message = getMergeRequestInstanceByCode(typeCode);
            message.decode(byteBuffer);
            msgs.add((AbstractMessage) message);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("FescarMergeMessage ");
        for (AbstractMessage msg : msgs) {
            sb.append(msg.toString()).append("\n");
        }
        return sb.toString();
    }
}
