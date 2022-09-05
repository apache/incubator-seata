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
package io.seata.core.rpc.netty.gts.message;

import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;

public class ClusterDumpMessage extends TxcMessage {
    private static final long serialVersionUID = -6826254198463287830L;
    private boolean verbose;

    public ClusterDumpMessage() {
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public short getTypeCode() {
        return 113;
    }

    @Override
    public byte[] encode() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        byteBuffer.put((byte)(this.verbose ? 1 : 0));
        byteBuffer.flip();
        byte[] content = new byte[byteBuffer.limit()];
        byteBuffer.get(content);
        return content;
    }

    @Override
    public boolean decode(ByteBuf in) {
        if (in.readableBytes() < 1) {
            return false;
        } else {
            this.verbose = in.readByte() == 1;
            return true;
        }
    }
}
