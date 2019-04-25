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

import java.nio.ByteBuffer;

/**
 * The type Global lock query response.
 *
 * @author jimin.jm @alibaba-inc.com
 */
public class GlobalLockQueryResponse extends AbstractTransactionResponse {

    private boolean lockable = false;

    /**
     * Is lockable boolean.
     *
     * @return the boolean
     */
    public boolean isLockable() {
        return lockable;
    }

    /**
     * Sets lockable.
     *
     * @param lockable the lockable
     */
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
        byteBuffer.putShort((short)(lockable ? 1 : 0));
    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        super.decode(byteBuffer);
        this.lockable = byteBuffer.getShort() == 1;
    }
}
