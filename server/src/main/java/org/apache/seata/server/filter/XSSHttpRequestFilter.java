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
package org.apache.seata.server.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.ConfigurationKeys;
import org.apache.seata.core.exception.HttpRequestFilterException;
import org.apache.seata.core.rpc.netty.http.filter.HttpFilterContext;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestFilter;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.seata.common.ConfigurationKeys.SERVER_HTTP_FILTER_XSS_FILTER_KEYWORDS;
import static org.apache.seata.common.DefaultValues.DEFAULT_XSS_KEYWORDS;

/**
 * Filter to detect and block potential XSS attack vectors in HTTP request parameters.
 */
@LoadLevel(name = "XSS", order = 1)
public class XSSHttpRequestFilter implements HttpRequestFilter {
    /**
     * The constant CONFIG.
     */
    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    private final List<String> xssKeywords;

    private static final int MAX_EVENT_HANDLER_LENGTH = 50;

    private static final int ON_REPEAT_LIMIT = 5;

    private static final Pattern ON_REPEAT_PATTERN =
            Pattern.compile("(on){" + ON_REPEAT_LIMIT + ",}", Pattern.CASE_INSENSITIVE);

    private static final Pattern EVENT_HANDLER_PATTERN = Pattern.compile(
            "\\bon([a-zA-Z0-9]{1," + MAX_EVENT_HANDLER_LENGTH + "}?)\\s*=\\s*['\"][^'\"]*['\"]",
            Pattern.CASE_INSENSITIVE);

    public XSSHttpRequestFilter() {
        String xssKeywordConfig = CONFIG.getConfig(SERVER_HTTP_FILTER_XSS_FILTER_KEYWORDS, null);

        if (StringUtils.isBlank(xssKeywordConfig)) {
            this.xssKeywords = DEFAULT_XSS_KEYWORDS;
        } else {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                xssKeywords = objectMapper.readValue(xssKeywordConfig, new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException(
                        "Invalid format for configuration 'server.http.filter.xss.keywords'. "
                                + "Expected a JSON array like [\"<script>\", \"vbscript:\"], but got: "
                                + xssKeywordConfig,
                        e);
            }
        }
    }

    @Override
    public int getOrder() {
        return 1;
    }

    /**
     * Checks all request parameters for XSS risks and throws if found.
     */
    @Override
    public void doFilter(HttpFilterContext<?> context) throws HttpRequestFilterException {
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

        for (String keyword : xssKeywords) {
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
