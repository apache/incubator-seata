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
package io.seata.core.rpc.processor;

import java.util.Map;

import io.seata.core.protocol.RpcMessage;

/**
 * @author goodboycoder
 */
public class MessageMeta {
    private int messageId;
    private byte messageType;
    private byte codec;
    private byte compressor;
    private Map<String, String> headMap;

    public MessageMeta() {
    }

    public MessageMeta(RpcMessage rpcMessage) {
        this.messageId = rpcMessage.getId();
        this.messageType = rpcMessage.getMessageType();
        this.codec = rpcMessage.getCodec();
        this.compressor = rpcMessage.getCompressor();
        this.headMap = rpcMessage.getHeadMap();
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public byte getMessageType() {
        return messageType;
    }

    public void setMessageType(byte messageType) {
        this.messageType = messageType;
    }

    public byte getCodec() {
        return codec;
    }

    public void setCodec(byte codec) {
        this.codec = codec;
    }

    public byte getCompressor() {
        return compressor;
    }

    public void setCompressor(byte compressor) {
        this.compressor = compressor;
    }

    public Map<String, String> getHeadMap() {
        return headMap;
    }

    public void setHeadMap(Map<String, String> headMap) {
        this.headMap = headMap;
    }

    @Override
    public String toString() {
        return "MessageMeta{" +
                "messageId=" + messageId +
                ", messageType=" + messageType +
                ", codec=" + codec +
                ", compressor=" + compressor +
                ", headMap=" + headMap +
                '}';
    }
}
