package org.apache.seata.core.rpc.netty.http.filter.impl;

import io.netty.handler.codec.http.HttpRequest;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.rpc.netty.http.filter.FilterException;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestFilter;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestParamWrapper;

import java.util.List;
import java.util.Map;

public class XssHttpRequestFilter implements HttpRequestFilter {

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public void filter(HttpRequest request, HttpRequestParamWrapper paramWrapper) throws FilterException {
        Map<String, List<String>> allParams = paramWrapper.getAllParamsAsMultiMap();
        for (Map.Entry<String, List<String>> entry : allParams.entrySet()) {
            for (String value : entry.getValue()) {
                if (value != null && containsXssRisk(value)) {
                    throw new FilterException("XSS risk detected in param: " + entry.getKey() + ", value: " + value);
                }
            }
        }
    }

    @Override
    public boolean shouldFilter() {
        String configKey = "server.http.filter.xss.enabled";
        String configValue = ConfigurationFactory.getInstance().getConfig(configKey);

        return configValue == null || !"false".equalsIgnoreCase(configValue);
    }

    private boolean containsXssRisk(String value) {
        String lower = value.toLowerCase();
        return lower.contains("<script>") || lower.contains("</script>") || lower.contains("onerror") || lower.contains("onload");
    }
}
