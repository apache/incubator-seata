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
import java.io.Serializable;
import java.nio.ByteBuffer;

public class RedressResultMessage extends TxcMessage implements Serializable {
    private static final long serialVersionUID = 5085244998017742274L;
    private boolean result;

    public RedressResultMessage() {
        this.result = true;
    }

    public RedressResultMessage(boolean result) {
        this.result = result;
    }

    public boolean isResult() {
        return this.result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    @Override
    public short getTypeCode() {
        return 122;
    }

    @Override
    public byte[] encode() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        byteBuffer.put((byte)(this.result ? 1 : 0));
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
            this.result = in.readByte() == 1;
            return true;
        }
    }
}
