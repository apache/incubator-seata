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

import java.util.HashMap;
import java.util.Map;

/**
 * The type Rpc message.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /9/14
 */
public class RpcMessage {

    private int id;
    private byte messageType;
    private byte codec;
    private byte compressor;
    private Map<String, String> headMap = new HashMap<>();
    private Object body;

    /**
     * Gets id.
     *
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * Sets id.
     *
     * @param id the id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Gets body.
     *
     * @return the body
     */
    public Object getBody() {
        return body;
    }

    /**
     * Sets body.
     *
     * @param body the body
     */
    public void setBody(Object body) {
        this.body = body;
    }

    /**
     * Gets codec.
     *
     * @return the codec
     */
    public byte getCodec() {
        return codec;
    }

    /**
     * Sets codec.
     *
     * @param codec the codec
     * @return the codec
     */
    public RpcMessage setCodec(byte codec) {
        this.codec = codec;
        return this;
    }

    /**
     * Gets compressor.
     *
     * @return the compressor
     */
    public byte getCompressor() {
        return compressor;
    }

    /**
     * Sets compressor.
     *
     * @param compressor the compressor
     * @return the compressor
     */
    public RpcMessage setCompressor(byte compressor) {
        this.compressor = compressor;
        return this;
    }

    /**
     * Gets head map.
     *
     * @return the head map
     */
    public Map<String, String> getHeadMap() {
        return headMap;
    }

    /**
     * Sets head map.
     *
     * @param headMap the head map
     * @return the head map
     */
    public RpcMessage setHeadMap(Map<String, String> headMap) {
        this.headMap = headMap;
        return this;
    }

    /**
     * Gets head.
     *
     * @param headKey the head key
     * @return the head
     */
    public String getHead(String headKey) {
        return headMap.get(headKey);
    }

    /**
     * Put head.
     *
     * @param headKey   the head key
     * @param headValue the head value
     */
    public void putHead(String headKey, String headValue) {
        headMap.put(headKey, headValue);
    }

    /**
     * Gets message type.
     *
     * @return the message type
     */
    public byte getMessageType() {
        return messageType;
    }

    /**
     * Sets message type.
     *
     * @param messageType the message type
     */
    public void setMessageType(byte messageType) {
        this.messageType = messageType;
    }
}
