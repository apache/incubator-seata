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

import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.ConfigurationKeys;
import org.apache.seata.core.exception.HttpRequestFilterException;
import org.apache.seata.core.rpc.netty.http.filter.HttpFilterContext;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestFilter;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
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

    private static final int MAX_EVENT_HANDLER_LENGTH = 50;

    private static final int ON_REPEAT_LIMIT = 5;

    private static final Pattern ON_REPEAT_PATTERN = Pattern.compile("(on){" + ON_REPEAT_LIMIT + ",}", Pattern.CASE_INSENSITIVE);

    private static final Pattern EVENT_HANDLER_PATTERN =
            Pattern.compile("\\bon([a-zA-Z0-9]{1," + MAX_EVENT_HANDLER_LENGTH + "}?)\\s*=\\s*['\"][^'\"]*['\"]", Pattern.CASE_INSENSITIVE);

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
                if (containsXssRisk(value)) {
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
        return ConfigurationFactory.getInstance()
                .getBoolean(ConfigurationKeys.SERVER_HTTP_FILTER_XSS_FILTER_ENABLE, true);
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

        if (ON_REPEAT_PATTERN.matcher(value).find()) {
            return true;
        }

        Matcher matcher = EVENT_HANDLER_PATTERN.matcher(value);
        while (matcher.find()) {
            String eventName = matcher.group(1);
            if (eventName.length() > MAX_EVENT_HANDLER_LENGTH) {
                return true;
            }
            return true;
        }
        return false;
    }
}
