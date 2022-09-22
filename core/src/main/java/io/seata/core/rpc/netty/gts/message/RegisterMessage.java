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

public class RegisterMessage extends TxcMessage implements MergedMessage {
    private static final long serialVersionUID = 1242711598812634704L;
    long tranId;
    String key;
    String businessKey;
    byte commitMode;
    public ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

    public RegisterMessage() {
    }

    public long getTranId() {
        return this.tranId;
    }

    public void setTranId(long tranId) {
        this.tranId = tranId;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public byte getCommitMode() {
        return this.commitMode;
    }

    public void setCommitMode(byte commitMode) {
        this.commitMode = commitMode;
    }

    public String getBusinessKey() {
        return this.businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    @Override
    public String toString() {
        String s = this.businessKey;
        if (this.businessKey != null && this.businessKey.length() > 1024) {
            s = this.businessKey.substring(0, 1024) + "...(length:" + this.businessKey.length() + ")";
        }

        return "RegisterMessage key:" + this.key + " tranId:" + this.tranId + " Commit mode:" + this.commitMode + " business key:" + s;
    }

    @Override
    public short getTypeCode() {
        return 11;
    }

    @Override
    public byte[] encode() {
        byte[] businessKeyBs = null;
        if (this.businessKey != null) {
            businessKeyBs = this.businessKey.getBytes(UTF8);
            if (businessKeyBs.length > 512) {
                this.byteBuffer = ByteBuffer.allocate(businessKeyBs.length + 1024);
            }
        }

        this.byteBuffer.putLong(this.tranId);
        this.byteBuffer.put(this.commitMode);
        byte[] content;
        if (this.key != null) {
            content = this.key.getBytes(UTF8);
            this.byteBuffer.putShort((short)content.length);
            if (content.length > 0) {
                this.byteBuffer.put(content);
            }
        } else {
            this.byteBuffer.putShort((short)0);
        }

        if (this.businessKey != null) {
            this.byteBuffer.putInt(businessKeyBs.length);
            if (businessKeyBs.length > 0) {
                this.byteBuffer.put(businessKeyBs);
            }
        } else {
            this.byteBuffer.putInt(0);
        }

        this.byteBuffer.flip();
        content = new byte[this.byteBuffer.limit()];
        this.byteBuffer.get(content);
        return content;
    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        this.tranId = byteBuffer.getLong();
        this.commitMode = byteBuffer.get();
        short len = byteBuffer.getShort();
        if (len > 0) {
            byte[] bs = new byte[len];
            byteBuffer.get(bs);
            this.setKey(new String(bs, UTF8));
        }

        int iLen = byteBuffer.getInt();
        if (iLen > 0) {
            byte[] bs = new byte[iLen];
            byteBuffer.get(bs);
            this.setBusinessKey(new String(bs, UTF8));
        }

    }
}
