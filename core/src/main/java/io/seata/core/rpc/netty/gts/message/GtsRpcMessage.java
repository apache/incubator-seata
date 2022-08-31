package io.seata.core.rpc.netty.gts.message;

import java.util.concurrent.atomic.AtomicLong;

public class GtsRpcMessage {
    private static AtomicLong NEXT_ID = new AtomicLong(0L);
    private long id;
    private boolean isAsync;
    private boolean isRequest;
    private boolean isHeartbeat;
    private Object body;

    public GtsRpcMessage() {
    }

    public static long getNextMessageId() {
        return NEXT_ID.incrementAndGet();
    }

    public boolean isAsync() {
        return this.isAsync;
    }

    public void setAsync(boolean isAsync) {
        this.isAsync = isAsync;
    }

    public boolean isRequest() {
        return this.isRequest;
    }

    public void setRequest(boolean isRequest) {
        this.isRequest = isRequest;
    }

    public boolean isHeartbeat() {
        return this.isHeartbeat;
    }

    public void setHeartbeat(boolean isHeartbeat) {
        this.isHeartbeat = isHeartbeat;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Object getBody() {
        return this.body;
    }

    public void setBody(Object body) {
        this.body = body;
    }
}
