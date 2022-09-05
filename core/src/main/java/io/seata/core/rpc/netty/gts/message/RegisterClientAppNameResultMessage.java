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
// TODO transform version between gts and seata
import io.seata.core.protocol.Version;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class RegisterClientAppNameResultMessage extends TxcMessage implements Serializable {
    private static final long serialVersionUID = 3629846050062228749L;
    private String version = Version.getCurrent();
    private boolean result;
    public ByteBuffer byteBuffer = ByteBuffer.allocate(256);

    public RegisterClientAppNameResultMessage() {
        this.result = true;
    }

    public RegisterClientAppNameResultMessage(boolean result) {
        this.result = result;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isResult() {
        return this.result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    @Override
    public short getTypeCode() {
        return 102;
    }

    @Override
    public byte[] encode() {
        this.byteBuffer.put((byte)(this.result ? 1 : 0));
        byte[] bs;
        if (this.version != null) {
            bs = this.version.getBytes(UTF8);
            this.byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                this.byteBuffer.put(bs);
            }
        } else {
            this.byteBuffer.putShort((short)0);
        }

        this.byteBuffer.flip();
        bs = new byte[this.byteBuffer.limit()];
        this.byteBuffer.get(bs);
        return bs;
    }

    @Override
    public boolean decode(ByteBuf in) {
        int i = in.readableBytes();
        if (i < 3) {
            return false;
        } else {
            i -= 3;
            this.result = in.readByte() == 1;
            short len = in.readShort();
            if (len > 0) {
                if (i < len) {
                    return false;
                }

                byte[] bs = new byte[len];
                in.readBytes(bs);
                this.setVersion(new String(bs, UTF8));
            }

            return true;
        }
    }
}
