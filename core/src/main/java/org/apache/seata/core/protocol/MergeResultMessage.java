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
package org.apache.seata.core.protocol;


/**
 * The type Merge result message.
 *
 */
public class MergeResultMessage extends AbstractMessage implements MergeMessage {

    /**
     * The Msgs.
     */
    public AbstractResultMessage[] msgs;

    /**
     * Get msgs abstract result message [ ].
     *
     * @return the abstract result message [ ]
     */
    public AbstractResultMessage[] getMsgs() {
        return msgs;
    }

    /**
     * Sets msgs.
     *
     * @param msgs the msgs
     */
    public void setMsgs(AbstractResultMessage[] msgs) {
        this.msgs = msgs;
    }

    @Override
    public short getTypeCode() {
        return MessageType.TYPE_SEATA_MERGE_RESULT;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MergeResultMessage ");
        if (msgs == null) {
            return sb.toString();
        }
        for (AbstractMessage msg : msgs) { sb.append(msg.toString()).append("\n"); }
        return sb.toString();
    }
}
