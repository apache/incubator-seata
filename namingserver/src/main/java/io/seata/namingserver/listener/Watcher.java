package io.seata.namingserver.listener;


import static io.seata.namingserver.listener.Watcher.Protocol.HTTP;


public class Watcher<T> {

    private String group;

    private volatile boolean done = false;

    private T asyncContext;

    private long timeout;

    private long term;

    private String protocol = HTTP;

    public Watcher(String group, T asyncContext, int timeout, long term) {
        this.group = group;
        this.asyncContext = asyncContext;
        this.timeout = System.currentTimeMillis() + timeout;
        this.term = term;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public T getAsyncContext() {
        return asyncContext;
    }

    public void setAsyncContext(T asyncContext) {
        this.asyncContext = asyncContext;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    public interface Protocol {
        String GRPC = "grpc";
        String HTTP = "http";
    }

}
