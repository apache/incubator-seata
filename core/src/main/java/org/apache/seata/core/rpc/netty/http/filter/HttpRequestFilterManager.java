package org.apache.seata.core.rpc.netty.http.filter;

import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.rpc.netty.http.filter.impl.XssHttpRequestFilter;

import java.util.ArrayList;
import java.util.List;

public class HttpRequestFilterManager {

    private static final List<HttpRequestFilter> filters = new ArrayList<>();
    private static final HttpRequestFilterChain filterChain;

    static {
        boolean globalEnabled = ConfigurationFactory.getInstance().getBoolean("server.http.filters.enabled", true);

        if (globalEnabled) {
            // 注册所有 filter，
            addIfEnabled(new XssHttpRequestFilter());

        }

        filters.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));
        filterChain = new HttpRequestFilterChain(filters);
    }

    private static void addIfEnabled(HttpRequestFilter filter) {
        if (filter.shouldFilter()) {
            filters.add(filter);
        }
    }

    public static HttpRequestFilterChain getFilterChain() {
        return filterChain;
    }
}
