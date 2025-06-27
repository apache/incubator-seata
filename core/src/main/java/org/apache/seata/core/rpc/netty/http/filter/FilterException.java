package org.apache.seata.core.rpc.netty.http.filter;

public class FilterException extends RuntimeException {
    public FilterException(String message) {
        super(message);
    }

    public FilterException(String message, Throwable cause) {
        super(message, cause);
    }
}
