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
package org.apache.seata.common.json;

import org.apache.seata.common.exception.JsonParseException;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.metadata.ClusterWatchEvent;
import org.apache.seata.common.metadata.MetadataResponse;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test-only codec that makes common-module JSON SPI usage observable.
 */
@LoadLevel(name = "testing")
public class TestingJsonCodec implements JsonCodec {

    public static final String SERIALIZED_PREFIX = "{\"codec\":\"testing\"";

    private static final Pattern GROUP_PATTERN = Pattern.compile("\"group\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\"timestamp\"\\s*:\\s*(\\d+)");
    private static final Pattern TERM_PATTERN = Pattern.compile("\"term\"\\s*:\\s*(\\d+)");
    private static final Pattern NULL_METADATA_PATTERN = Pattern.compile("\"metadata\"\\s*:\\s*null");

    @Override
    public String toJSONString(Object object) {
        StringBuilder json = new StringBuilder(SERIALIZED_PREFIX);
        if (object instanceof Map) {
            for (Object entryObject : ((Map<?, ?>) object).entrySet()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entryObject;
                json.append(",\"")
                        .append(entry.getKey())
                        .append("\":\"")
                        .append(entry.getValue())
                        .append("\"");
            }
        }
        return json.append('}').toString();
    }

    @Override
    public <T> T parseObject(String text, Class<T> clazz) {
        if (!ClusterWatchEvent.class.equals(clazz)) {
            throw new JsonParseException("Unsupported test parse type: " + clazz);
        }
        if (text.startsWith("codec:")) {
            return clazz.cast(parseCodecFormat(text));
        }
        ClusterWatchEvent event = new ClusterWatchEvent();
        event.setGroup(findString(GROUP_PATTERN, text));
        event.setTimestamp(findLong(TIMESTAMP_PATTERN, text));
        if (!NULL_METADATA_PATTERN.matcher(text).find()) {
            MetadataResponse metadata = new MetadataResponse();
            metadata.setTerm(findLong(TERM_PATTERN, text));
            event.setMetadata(metadata);
        }
        return clazz.cast(event);
    }

    private static ClusterWatchEvent parseCodecFormat(String text) {
        ClusterWatchEvent event = new ClusterWatchEvent();
        MetadataResponse metadata = new MetadataResponse();
        String[] parts = text.substring("codec:".length()).split(",");
        for (String part : parts) {
            String[] nameAndValue = part.split("=", 2);
            if (nameAndValue.length != 2) {
                throw new JsonParseException("Unable to parse test codec string");
            }
            if ("group".equals(nameAndValue[0])) {
                event.setGroup(nameAndValue[1]);
            } else if ("timestamp".equals(nameAndValue[0])) {
                event.setTimestamp(Long.parseLong(nameAndValue[1]));
            } else if ("term".equals(nameAndValue[0])) {
                metadata.setTerm(Long.parseLong(nameAndValue[1]));
            }
        }
        event.setMetadata(metadata);
        return event;
    }

    private static String findString(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            throw new JsonParseException("Unable to parse test JSON string");
        }
        return matcher.group(1);
    }

    private static Long findLong(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            throw new JsonParseException("Unable to parse test JSON number");
        }
        return Long.parseLong(matcher.group(1));
    }
}
