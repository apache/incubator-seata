package io.seata.core.rpc.processor;

import java.util.Map;

import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.SeataChannel;

/**
 * @author goodboycoder
 */
public abstract class RpcMessageHandlerContext {
    private SeataChannel channel;

    private int messageId;
    private byte messageType;
    private byte codec;
    private byte compressor;
    private Map<String, String> headMap;

    public RpcMessageHandlerContext(RpcMessage rpcMessage) {
        this.codec = rpcMessage.getCodec();
        this.compressor = rpcMessage.getCompressor();
        this.headMap = rpcMessage.getHeadMap();
        this.messageId = rpcMessage.getId();
    }

    public SeataChannel channel() {
        return channel;
    }

    public void setChannel(SeataChannel channel) {
        this.channel = channel;
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

    public abstract void close();
    public abstract void disconnect();
}
