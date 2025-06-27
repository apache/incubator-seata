package org.apache.seata.core.rpc.netty.http.filter;

import io.netty.handler.codec.http.HttpRequest;

import java.util.List;

public class HttpRequestFilterChain {
    private final List<HttpRequestFilter> filters;

    public HttpRequestFilterChain(List<HttpRequestFilter> filters) {
        this.filters = filters;
    }

    public void doFilter(HttpRequest request, HttpRequestParamWrapper paramWrapper) throws FilterException {
        for (HttpRequestFilter filter : filters) {
            filter.filter(request, paramWrapper);
        }
    }
}
