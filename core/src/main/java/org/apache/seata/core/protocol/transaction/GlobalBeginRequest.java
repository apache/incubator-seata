/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.core.protocol.transaction;

import org.apache.seata.core.protocol.MessageType;
import org.apache.seata.core.rpc.RpcContext;

/**
 * The type Global begin request.
 *
 */
public class GlobalBeginRequest extends AbstractTransactionRequestToTC {

    private int timeout = 60000;

    private String transactionName;

    /**
     * Gets timeout.
     *
     * @return the timeout
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Sets timeout.
     *
     * @param timeout the timeout
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Gets transaction name.
     *
     * @return the transaction name
     */
    public String getTransactionName() {
        return transactionName;
    }

    /**
     * Sets transaction name.
     *
     * @param transactionName the transaction name
     */
    public void setTransactionName(String transactionName) {
        this.transactionName = transactionName;
    }

    @Override
    public short getTypeCode() {
        return MessageType.TYPE_GLOBAL_BEGIN;
    }


    @Override
    public AbstractTransactionResponse handle(RpcContext rpcContext) {
        return handler.handle(this, rpcContext);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GlobalBeginRequest{");
        sb.append("transactionName='").append(transactionName).append('\'');
        sb.append(", timeout=").append(timeout);
        sb.append('}');
        return sb.toString();
    }
}
