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
package io.seata.core.protocol;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The type Merged warp message.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /10/9
 */
public class MergedWarpMessage extends AbstractMessage implements Serializable, MergeMessage {
    private static final long serialVersionUID = -5758802337446717090L;

    /**
     * The Msgs.
     */
    public List<AbstractMessage> msgs = new ArrayList<>();
    /**
     * The Msg ids.
     */
    public List<Long> msgIds = new ArrayList<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(MergedWarpMessage.class);

    @Override
    public short getTypeCode() {
        return TYPE_SEATA_MERGE;
    }

    @Override
    public byte[] encode() {
        final ByteBuf buffer = Unpooled.buffer(1024);
        buffer.writeInt(0); // write placeholder for content length

        buffer.writeShort((short) msgs.size());
        for (final AbstractMessage msg : msgs) {
            final byte[] bytes = msg.encode();
            buffer.writeShort(msg.getTypeCode());
            buffer.writeBytes(bytes);
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
        return content;
    }

    @Override
    public boolean decode(ByteBuf in) {
        if (in.readableBytes() < 4) {
            return false;
        }

        int length = in.readInt();
        if (in.readableBytes() < length) {
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
            msgs.add((AbstractMessage)message);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SeataMergeMessage ");
        for (AbstractMessage msg : msgs) {
            sb.append(msg.toString()).append("\n");
        }
        return sb.toString();
    }
}
