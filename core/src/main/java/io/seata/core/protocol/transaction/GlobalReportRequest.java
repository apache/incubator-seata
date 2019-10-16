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

import io.seata.core.model.GlobalStatus;
import io.seata.core.protocol.MessageType;
import io.seata.core.rpc.RpcContext;

import java.nio.ByteBuffer;

/**
 * The type Global report request.
 *
 * @author lorne.cl
 */
public class GlobalReportRequest extends AbstractGlobalEndRequest {

    /**
     * The Global status.
     */
    protected GlobalStatus globalStatus;

    @Override
    public short getTypeCode() {
        return MessageType.TYPE_GLOBAL_REPORT;
    }

    @Override
    public AbstractTransactionResponse handle(RpcContext rpcContext) {
        return handler.handle(this, rpcContext);
    }

    /**
     * Gets global status.
     *
     * @return the global status
     */
    public GlobalStatus getGlobalStatus() {
        return globalStatus;
    }

    /**
     * Sets global status.
     *
     * @param globalStatus the global status
     */
    public void setGlobalStatus(GlobalStatus globalStatus) {
        this.globalStatus = globalStatus;
    }
}
