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

import java.nio.ByteBuffer;

public class BeginResultMessage extends AbstractResultMessage {
    private static final long serialVersionUID = -5947172130577163908L;
    String xid;
    String nextSvrAddr;

    public BeginResultMessage() {
    }

    public String getXid() {
        return this.xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public String getNextSvrAddr() {
        return this.nextSvrAddr;
    }

    public void setNextSvrAddr(String nextSvrAddr) {
        this.nextSvrAddr = nextSvrAddr;
    }

    @Override
    public String toString() {
        return "BeginResultMessage result:" + this.result + " xid:" + this.xid;
    }

    @Override
    public short getTypeCode() {
        return 2;
    }

    @Override
    public byte[] encode() {
        super.encode();
        byte[] bs;
        if (this.xid != null) {
            bs = this.xid.getBytes(UTF8);
            this.byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                this.byteBuffer.put(bs);
            }
        } else {
            this.byteBuffer.putShort((short)0);
        }

        if (this.nextSvrAddr != null) {
            bs = this.nextSvrAddr.getBytes(UTF8);
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
    public void decode(ByteBuffer byteBuffer) {
        super.decode(byteBuffer);
        short len = byteBuffer.getShort();
        byte[] bs;
        if (len > 0) {
            bs = new byte[len];
            byteBuffer.get(bs);
            this.setXid(new String(bs, UTF8));
        }

        len = byteBuffer.getShort();
        if (len > 0) {
            bs = new byte[len];
            byteBuffer.get(bs);
            this.setNextSvrAddr(new String(bs, UTF8));
        }

    }
}
