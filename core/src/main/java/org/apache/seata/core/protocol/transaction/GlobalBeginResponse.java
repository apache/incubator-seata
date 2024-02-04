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

/**
 * The type Global begin response.
 *
 */
public class GlobalBeginResponse extends AbstractTransactionResponse {

    private String xid;

    private String extraData;

    /**
     * Gets xid.
     *
     * @return the xid
     */
    public String getXid() {
        return xid;
    }

    /**
     * Sets xid.
     *
     * @param xid the xid
     */
    public void setXid(String xid) {
        this.xid = xid;
    }

    /**
     * Gets extra data.
     *
     * @return the extra data
     */
    public String getExtraData() {
        return extraData;
    }

    /**
     * Sets extra data.
     *
     * @param extraData the extra data
     */
    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

    @Override
    public short getTypeCode() {
        return MessageType.TYPE_GLOBAL_BEGIN_RESULT;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GlobalBeginResponse{");
        sb.append("xid='").append(xid).append('\'');
        sb.append(", extraData='").append(extraData).append('\'');
        sb.append(", resultCode=").append(resultCode);
        sb.append(", msg='").append(msg).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
