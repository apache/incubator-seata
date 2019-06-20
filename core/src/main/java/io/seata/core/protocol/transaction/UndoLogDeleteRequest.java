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
package io.seata.core.protocol.transaction;

import io.netty.buffer.ByteBuf;
import io.seata.core.model.BranchType;
import io.seata.core.rpc.RpcContext;

import java.io.Serializable;

/**
 * The type to delete undolog  request.
 *
 * @author github-ygy
 * @date 2019-6-14
 */
public class UndoLogDeleteRequest extends AbstractTransactionRequestToRM implements Serializable {

    private static final long serialVersionUID = 7539732523682335742L;

    public static final int DEFAULT_SAVE_DAYS = 7;

    private String resourceId;

    private int saveDays = DEFAULT_SAVE_DAYS;

    /**
     * The Branch type.
     */
    protected BranchType branchType = BranchType.AT;

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public int getSaveDays() {
        return saveDays;
    }

    public void setSaveDays(int saveDays) {
        this.saveDays = saveDays;
    }

    public BranchType getBranchType() {
        return branchType;
    }

    public void setBranchType(BranchType branchType) {
        this.branchType = branchType;
    }

    @Override
    public AbstractTransactionResponse handle(RpcContext rpcContext) {
        handler.handle(this);
        return null;
    }

    @Override
    public short getTypeCode() {
        return TYPE_RM_DELETE_UNDOLOG;
    }

    @Override
    public byte[] encode() {

        // 1. Branch Type
        byteBuffer.put((byte)this.branchType.ordinal());
        // 2. Resource Id
        if (this.resourceId != null) {
            byte[] bs = resourceId.getBytes(UTF8);
            byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                byteBuffer.put(bs);
            }
        } else {
            byteBuffer.putShort((short)0);
        }
        //3.save days
        byteBuffer.putInt(saveDays);

        byteBuffer.flip();
        byte[] content = new byte[byteBuffer.limit()];
        byteBuffer.get(content);
        return content;
    }

    @Override
    public boolean decode(ByteBuf in) {

        if (in.readableBytes() < 1) {
            return false;
        }
        this.branchType = BranchType.get(in.readByte());

        if (in.readableBytes() < 2) {
            return false;
        }
        int resourceIdLen = in.readShort();

        if (resourceIdLen <= 0 || in.readableBytes() < resourceIdLen) {
            return false;
        }

        byte[] bs = new byte[resourceIdLen];
        in.readBytes(bs);
        setResourceId(new String(bs, UTF8));

        if (in.readableBytes() < 4) {
            return false;
        }
        this.saveDays = in.readInt();
        return true;
    }
}
