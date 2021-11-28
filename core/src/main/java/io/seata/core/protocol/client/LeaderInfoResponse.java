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
package io.seata.core.protocol.client;

import java.io.Serializable;

import io.seata.core.protocol.MessageType;
import io.seata.core.protocol.transaction.AbstractTransactionResponse;

/**
 * @author funkye
 */
public class LeaderInfoResponse extends AbstractTransactionResponse implements Serializable {

    private String address;

    private String mode;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    @Override
    public short getTypeCode() {
        return MessageType.TYPE_LEADER_INFO_RESULT;
    }

    @Override
    public String toString() {
        return "LeaderInfoResponse{" + "address='" + address + '\'' + ", mode='" + mode + '\'' + '}';
    }
    
}
