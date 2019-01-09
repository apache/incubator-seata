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

package com.alibaba.fescar.core.protocol.transaction;

import java.nio.ByteBuffer;

import com.alibaba.fescar.core.protocol.AbstractMessage;

public class GlobalBeginResponse extends AbstractTransactionResponse {

    private static final long serialVersionUID = -5947172130577163908L;

    private String xid;

    private String extraData;

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public String getExtraData() {
        return extraData;
    }

    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

    @Override
    public short getTypeCode() {
        return AbstractMessage.TYPE_GLOBAL_BEGIN_RESULT;
    }

    @Override
    protected void doEncode() {
        super.doEncode();

        if (this.xid != null) {
            byte[] bs = xid.getBytes(UTF8);
            byteBuffer.putShort((short) bs.length);
            if (bs.length > 0) {
                byteBuffer.put(bs);
            }
        } else {
            byteBuffer.putShort((short) 0);
        }

        if (this.extraData != null) {
            byte[] bs = extraData.getBytes(UTF8);
            byteBuffer.putShort((short) bs.length);
            if (bs.length > 0) {
                byteBuffer.put(bs);
            }
        } else {
            byteBuffer.putShort((short) 0);
        }
    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        super.decode(byteBuffer);

        short len = byteBuffer.getShort();
        if (len > 0) {
            byte[] bs = new byte[len];
            byteBuffer.get(bs);
            setXid(new String(bs, UTF8));
        }

        len = byteBuffer.getShort();
        if (len > 0) {
            byte[] bs = new byte[len];
            byteBuffer.get(bs);
            setExtraData(new String(bs, UTF8));
        }
    }
}
