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
