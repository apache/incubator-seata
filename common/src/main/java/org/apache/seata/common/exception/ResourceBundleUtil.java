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
package org.apache.seata.common.exception;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.seata.common.loader.EnhancedServiceLoader;

public class ResourceBundleUtil {

    private static final Locale LOCALE = new Locale("en", "US");
    private static final ResourceBundleUtil INSTANCE = new ResourceBundleUtil("error/ErrorCode", LOCALE);
    private ResourceBundle localBundle;
    private AbstractRemoteResourceBundle remoteBundle;

    public static final String DEFAULT_PLACEHOLDER_PREFIX = "${";
    public static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";

    public static ResourceBundleUtil getInstance() {
        return INSTANCE;
    }

    public ResourceBundleUtil(String bundleName, Locale local) {

        this.localBundle = ResourceBundle.getBundle(bundleName, local);
        try {
            this.remoteBundle = EnhancedServiceLoader.load(AbstractRemoteResourceBundle.class);
        } catch (Throwable e) {
            //ignore
        }
    }

    public String getMessage(String key, String... params) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(getFormattedMessage(key));
        String msg = parseStringValue(sb.toString(), new HashSet<String>());
        if (params == null || params.length == 0) {
            return msg;
        }
        if (StringUtils.isBlank(msg)) {
            return msg;
        }
        return MessageFormat.format(msg, (Object[])params);
    }

    public String getMessage(String key, int code, String type, String... params) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();

        sb.append(getFormattedMessage("ERR_PREFIX")).append(" ").append(getFormattedMessage(key)).append(" ").append(
            getFormattedMessage("ERR_POSTFIX"));
        String msg = sb.toString();
        msg = parseStringValue(msg, new HashSet<String>());
        msg = StringUtils.replace(msg, "{code}", String.valueOf(code));
        msg = StringUtils.replace(msg, "{type}", String.valueOf(type));
        msg = StringUtils.replace(msg, "{key}", key);

        if (params == null || params.length == 0) {
            return msg;
        }
        if (StringUtils.isBlank(msg)) {
            return msg;
        }
        return MessageFormat.format(msg, (Object[])params);
    }

    protected String getFormattedMessage(String key) {
        String value = StringUtils.EMPTY;
        if (remoteBundle != null && remoteBundle.containsKey(key)) {
            value = remoteBundle.getString(key);
        }
        if (StringUtils.isEmpty(value)) {
            value = localBundle.getString(key);
        }
        return value;
    }

    protected String parseStringValue(String strVal, Set<String> visitedPlaceholders) {
        StringBuffer buf = new StringBuffer(strVal);
        int startIndex = strVal.indexOf(DEFAULT_PLACEHOLDER_PREFIX);
        while (startIndex != -1) {
            int endIndex = findPlaceholderEndIndex(buf, startIndex);
            if (endIndex != -1) {
                String placeholder = buf.substring(startIndex + DEFAULT_PLACEHOLDER_PREFIX.length(), endIndex);
                if (!visitedPlaceholders.add(placeholder)) {
                    throw new SeataRuntimeException(ErrorCode.ERR_CONFIG,
                        "Duplicate placeholders exist '" + placeholder + "' in bundle.");
                }
                placeholder = parseStringValue(placeholder, visitedPlaceholders);
                try {
                    String propVal = resolvePlaceholder(placeholder);
                    if (propVal != null) {
                        propVal = parseStringValue(propVal, visitedPlaceholders);
                        buf.replace(startIndex, endIndex + DEFAULT_PLACEHOLDER_SUFFIX.length(), propVal);
                        startIndex = buf.indexOf(DEFAULT_PLACEHOLDER_PREFIX, startIndex + propVal.length());
                    } else {
                        throw new SeataRuntimeException(ErrorCode.ERR_CONFIG,
                            "Could not resolve placeholder '" + placeholder + "'");
                    }
                } catch (Exception ex) {
                    throw new SeataRuntimeException(ErrorCode.ERR_CONFIG,
                        "Could not resolve placeholder '" + placeholder + "'");
                }
                visitedPlaceholders.remove(placeholder);
            } else {
                startIndex = -1;
            }
        }

        return buf.toString();
    }

    private int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
        int index = startIndex + DEFAULT_PLACEHOLDER_PREFIX.length();
        int withinNestedPlaceholder = 0;
        while (index < buf.length()) {
            if (matchSubString(buf, index, DEFAULT_PLACEHOLDER_SUFFIX)) {
                if (withinNestedPlaceholder > 0) {
                    withinNestedPlaceholder--;
                    index = index + DEFAULT_PLACEHOLDER_SUFFIX.length();
                } else {
                    return index;
                }
            } else if (matchSubString(buf, index, DEFAULT_PLACEHOLDER_PREFIX)) {
                withinNestedPlaceholder++;
                index = index + DEFAULT_PLACEHOLDER_PREFIX.length();
            } else {
                index++;
            }
        }
        return -1;
    }

    private boolean matchSubString(CharSequence str, int index, CharSequence substring) {
        for (int j = 0; j < substring.length(); j++) {
            int i = index + j;
            if (i >= str.length() || str.charAt(i) != substring.charAt(j)) {
                return false;
            }
        }
        return true;
    }

    private String resolvePlaceholder(String placeholder) {
        return getFormattedMessage(placeholder);
    }

}
