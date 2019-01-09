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

public class GlobalLockQueryResponse extends AbstractTransactionResponse {

    private boolean lockable = false;

    public boolean isLockable() {
        return lockable;
    }

    public void setLockable(boolean lockable) {
        this.lockable = lockable;
    }

    @Override
    public short getTypeCode() {
        return TYPE_GLOBAL_LOCK_QUERY_RESULT;
    }


    @Override
    protected void doEncode() {
        super.doEncode();
        byteBuffer.putShort((short) (lockable ? 1 : 0));
    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        super.decode(byteBuffer);
        this.lockable = byteBuffer.getShort() == 1;
    }
}
