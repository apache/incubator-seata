package org.apache.seata.core.rpc.netty.http.filter;

import io.netty.handler.codec.http.HttpRequest;

public interface HttpRequestFilter {

    /**
     * Filter order, lower value runs earlier
     */
    default int getOrder(){
        return 0;
    }

    /**
     * Main filter logic
     */
    void filter(HttpRequest request, HttpRequestParamWrapper paramWrapper) throws FilterException;

    boolean shouldFilter();

}
