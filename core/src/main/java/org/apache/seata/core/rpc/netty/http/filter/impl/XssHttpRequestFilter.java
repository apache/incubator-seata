package org.apache.seata.core.rpc.netty.http.filter.impl;

import io.netty.handler.codec.http.HttpRequest;
import org.apache.seata.core.rpc.netty.http.filter.FilterException;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestFilter;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestParamWrapper;

import java.util.Map;

public class XssHttpRequestFilter implements HttpRequestFilter {

    @Override
    public void filter(HttpRequest request, HttpRequestParamWrapper paramWrapper) throws FilterException {
        Map<String, String> allParams = paramWrapper.getAllParamsAsFlatMap();
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            String value = entry.getValue();
            if (value != null && containsXssRisk(value)) {
                throw new FilterException("XSS risk detected in param: " + entry.getKey());
            }
        }
    }

    private boolean containsXssRisk(String value) {
        String lower = value.toLowerCase();
        return lower.contains("<script") || lower.contains("</script>") || lower.contains("javascript:");
    }

    @Override
    public int getOrder() {
        return 1;
    }
}