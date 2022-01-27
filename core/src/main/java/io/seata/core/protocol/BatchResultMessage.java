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
package io.seata.core.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * The type batch result message.
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.5.0
 */
public class BatchResultMessage extends AbstractMessage {

    /**
     * the result messages
     */
    private List<AbstractResultMessage> resultMessages = new ArrayList<>();

    /**
     * the message Ids
     */
    private List<Integer> msgIds = new ArrayList<>();

    @Override
    public short getTypeCode() {
        return MessageType.TYPE_BATCH_RESULT_MSG;
    }

    public List<AbstractResultMessage> getResultMessages() {
        return resultMessages;
    }

    public void setResultMessages(List<AbstractResultMessage> resultMessages) {
        this.resultMessages = resultMessages;
    }

    public List<Integer> getMsgIds() {
        return msgIds;
    }

    public void setMsgIds(List<Integer> msgIds) {
        this.msgIds = msgIds;
    }
}
