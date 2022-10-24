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

import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.SeataChannel;

/**
 * @author goodboycoder
 */
public class RpcMessageHandleContext {
    private final SeataChannel channel;
    private MessageMeta messageMeta;

    private MessageReply messageReply;

    public RpcMessageHandleContext(RpcMessage rpcMessage, SeataChannel channel) {
        this.messageMeta = new MessageMeta(rpcMessage);
        this.channel = channel;
    }

    public RpcMessageHandleContext(SeataChannel channel) {
        this.channel = channel;
    }

    public SeataChannel channel() {
        return channel;
    }

    public MessageMeta getMessageMeta() {
        return messageMeta;
    }

    public void setMessageMeta(MessageMeta messageMeta) {
        this.messageMeta = messageMeta;
    }

    public MessageReply getMessageReply() {
        return messageReply;
    }

    public void setMessageReply(MessageReply messageReply) {
        this.messageReply = messageReply;
    }

    public void close() {
        // do nothing
    }
    public void disconnect(){
        // do nothing
    }
}
