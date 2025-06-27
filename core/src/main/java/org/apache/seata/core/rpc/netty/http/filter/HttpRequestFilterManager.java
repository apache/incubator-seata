package org.apache.seata.core.rpc.netty.http.filter;

import org.apache.seata.core.rpc.netty.http.filter.impl.XssHttpRequestFilter;

import java.util.ArrayList;
import java.util.List;

public class HttpRequestFilterManager {

    private static final List<HttpRequestFilter> filters = new ArrayList<>();
    private static final HttpRequestFilterChain filterChain;

    static {
        // 注册所有过滤器，并按照 order 排序
        filters.add(new XssHttpRequestFilter());

        filters.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));
        filterChain = new HttpRequestFilterChain(filters);
    }

    public static HttpRequestFilterChain getFilterChain() {
        return filterChain;
    }
}
