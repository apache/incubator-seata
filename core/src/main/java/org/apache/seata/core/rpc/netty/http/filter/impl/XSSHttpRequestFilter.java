/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.core.rpc.netty.http.filter.impl;

import io.netty.handler.codec.http.HttpRequest;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.ConfigurationKeys;
import org.apache.seata.core.exception.HttpRequestFilterException;
import org.apache.seata.core.rpc.netty.http.filter.HttpFilterContext;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestFilter;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestParamWrapper;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Filter to detect and block potential XSS attack vectors in HTTP request parameters.
 */
public class XSSHttpRequestFilter implements HttpRequestFilter {

    private static final String[] XSS_KEYWORDS = {
        "<script>",
        "</script>",
        "javascript:",
        "vbscript:",
        "data:",
        "expression(",
        "onerror",
        "onload",
        "onclick",
        "onmouseover",
        "onfocus",
        "onblur",
        "onmouseenter",
        "onmouseleave",
        "onkeydown",
        "onkeyup",
        "onchange",
        "<iframe>",
        "<img>",
        "<svg>",
        "<embed>",
        "<object>",
        "<style>",
        "<link>"
    };

    private static final Pattern EVENT_HANDLER_PATTERN =
            Pattern.compile("on\\w+\\s*=\\s*['\"].*?['\"]", Pattern.CASE_INSENSITIVE);

    @Override
    public int getOrder() {
        return 1;
    }

    /**
     * Checks all request parameters for XSS risks and throws if found.
     */
    @Override
    public void doFilter(HttpFilterContext context) throws HttpRequestFilterException {
        Map<String, List<String>> allParams = context.getParamWrapper().getAllParamsAsMultiMap();
        for (Map.Entry<String, List<String>> entry : allParams.entrySet()) {
            for (String value : entry.getValue()) {
                if (value != null && containsXssRisk(value)) {
                    throw new HttpRequestFilterException(
                            "XSS risk detected in param: " + entry.getKey() + ", value: " + value);
                }
            }
        }
    }

    /**
     * Returns whether this XSS filter is enabled via configuration.
     */
    @Override
    public boolean shouldApply() {
        return ConfigurationFactory.getInstance().getBoolean(ConfigurationKeys.SERVER_HTTP_FILTER_XSS_FILTER_ENABLE, true);
    }

    /**
     * Basic check for common XSS patterns in a string value.
     */
    private boolean containsXssRisk(String value) {
        if (value == null) {
            return false;
        }

        String normalized = value.toLowerCase().replaceAll("\\s+", "");

        for (String keyword : XSS_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }

        if (EVENT_HANDLER_PATTERN.matcher(value).find()) {
            return true;
        }

        return false;
    }
}
