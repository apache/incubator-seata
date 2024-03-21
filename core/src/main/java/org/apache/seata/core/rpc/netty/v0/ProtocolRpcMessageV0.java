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
package org.apache.seata.core.rpc.netty.v0;

import org.apache.seata.core.compressor.CompressorType;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.rpc.netty.ProtocolRpcMessage;
import org.apache.seata.core.serializer.SerializerType;

import java.util.concurrent.atomic.AtomicLong;

/**
 * the protocol v0 rpc message
 **/
public class ProtocolRpcMessageV0 implements ProtocolRpcMessage {

    private static AtomicLong NEXT_ID = new AtomicLong(0);

    /**
     * Gets next message id.
     *
     * @return the next message id
     */
    public static long getNextMessageId() {
        return NEXT_ID.incrementAndGet();
    }

    private long id;
    private boolean isAsync;
    private boolean isRequest;
    private boolean isHeartbeat;
    private Object body;
    private byte messageType;
    private boolean isSeataCodec;

    /**
     * Gets id.
     *
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * Sets id.
     *
     * @param id the id
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Is async boolean.
     *
     * @return the boolean
     */
    public boolean isAsync() {
        return isAsync;
    }

    /**
     * Sets async.
     *
     * @param async the async
     */
    public void setAsync(boolean async) {
        isAsync = async;
    }

    /**
     * Is request boolean.
     *
     * @return the boolean
     */
    public boolean isRequest() {
        return isRequest;
    }

    /**
     * Sets request.
     *
     * @param request the request
     */
    public void setRequest(boolean request) {
        isRequest = request;
    }

    /**
     * Is heartbeat boolean.
     *
     * @return the boolean
     */
    public boolean isHeartbeat() {
        return isHeartbeat;
    }

    /**
     * Sets heartbeat.
     *
     * @param heartbeat the heartbeat
     */
    public void setHeartbeat(boolean heartbeat) {
        isHeartbeat = heartbeat;
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

    public boolean isSeataCodec() {
        return isSeataCodec;
    }

    public void setSeataCodec(boolean seataCodec) {
        isSeataCodec = seataCodec;
    }

    public byte getMessageType() {
        return messageType;
    }

    public void setMessageType(byte messageType) {
        this.messageType = messageType;
    }

    @Override
    public RpcMessage protocolMsg2RpcMsg() {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setMessageType(this.messageType);
        rpcMessage.setCompressor(CompressorType.NONE.getCode());

        byte codecType = this.isSeataCodec ? SerializerType.SEATA.getCode() : SerializerType.HESSIAN.getCode();
        rpcMessage.setCodec(codecType);

        if (this.isHeartbeat) {
            if (this.isRequest) {
                rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST);
            } else {
                rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE);
            }
        } else {
            if (this.isRequest) {
                rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY);
            } else {
                rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_RESPONSE);
            }
        }
        rpcMessage.setBody(this.body);
        rpcMessage.setId((int) this.id);
        return rpcMessage;
    }

    @Override
    public void rpcMsg2ProtocolMsg(RpcMessage rpcMessage) {
        this.body = rpcMessage.getBody();
        this.id = rpcMessage.getId();
        this.isRequest = isRequest(rpcMessage.getMessageType());
        this.isHeartbeat = isHeartbeat(rpcMessage.getMessageType());
        this.isSeataCodec = rpcMessage.getCodec() == SerializerType.SEATA.getCode();
        this.messageType = rpcMessage.getMessageType();
    }

    private boolean isHeartbeat(byte msgType) {
        return msgType == ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST
                || msgType == ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE;
    }

    private boolean isRequest(byte msgType) {
        return msgType == ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY
                || msgType == ProtocolConstants.MSGTYPE_RESQUEST_SYNC;
    }
}
