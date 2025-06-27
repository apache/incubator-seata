package org.apache.seata.core.rpc.netty.http.filter;

import io.netty.handler.codec.http.HttpRequest;

public interface HttpRequestFilter {

    /**
     * Filter order, lower value runs earlier
     */
    int getOrder();

    /**
     * Main filter logic
     */
    void filter(HttpRequest request, HttpRequestParamWrapper paramWrapper) throws FilterException;
}
