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

public class GlobalRollbackResultMessage extends AbstractResultMessage {
    private static final long serialVersionUID = -2141370444891199652L;
    private long tranId;

    public GlobalRollbackResultMessage() {
    }

    public long getTranId() {
        return this.tranId;
    }

    public void setTranId(long tranId) {
        this.tranId = tranId;
    }

    @Override
    public String toString() {
        return "GlobalRollbackResultMessage tranId:" + this.tranId + ",result:" + this.result + ",msg:" + this.getMsg();
    }

    @Override
    public short getTypeCode() {
        return 10;
    }

    @Override
    public byte[] encode() {
        super.encode();
        this.byteBuffer.flip();
        byte[] content = new byte[this.byteBuffer.limit()];
        this.byteBuffer.get(content);
        return content;
    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        super.decode(byteBuffer);
    }
}
