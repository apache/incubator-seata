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

import java.io.Serializable;
import java.nio.ByteBuffer;

import com.alibaba.fescar.core.protocol.AbstractMessage;

/**
 * The type Branch register response.
 *
 * @author jimin.jm @alibaba-inc.com
 */
public class BranchRegisterResponse extends AbstractTransactionResponse implements Serializable {

    private static final long serialVersionUID = 8317040433102745774L;

    private String xid;

    private long branchId;

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    /**
     * Gets branch id.
     *
     * @return the branch id
     */
    public long getBranchId() {
        return branchId;
    }

    /**
     * Sets branch id.
     *
     * @param branchId the branch id
     */
    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    @Override
    public short getTypeCode() {
        return AbstractMessage.TYPE_BRANCH_REGISTER_RESULT;
    }

    @Override
    protected void doEncode() {
        super.doEncode();
        if (this.xid != null) {
            byte[] bs = xid.getBytes(UTF8);
            byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                byteBuffer.put(bs);
            }
        } else {
            byteBuffer.putShort((short)0);
        }
        byteBuffer.putLong(branchId);

    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        super.decode(byteBuffer);
        short xidlen = byteBuffer.getShort();
        if (xidlen > 0) {
            byte[] bs = new byte[xidlen];
            byteBuffer.get(bs);
            this.setXid(new String(bs, UTF8));
        }
        this.branchId = byteBuffer.getLong();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("BranchRegisterResponse: xid=");
        result.append(xid);
        result.append(",");
        result.append("branchId=");
        result.append(branchId);
        result.append(",");
        result.append("result code =");
        result.append(getResultCode());
        result.append(",");
        result.append("getMsg =");
        result.append(getMsg());

        return result.toString();
    }
}
