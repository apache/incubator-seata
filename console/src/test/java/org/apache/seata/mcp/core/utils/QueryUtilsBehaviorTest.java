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
package org.apache.seata.mcp.core.utils;

import org.apache.seata.mcp.core.props.NameSpaceDetail;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryUtilsBehaviorTest {
    @Test
    void datesRespectLocalZoneAndRejectMalformedInput() {
        assertTrue(DateUtils.isValidDate("2026-01-02"));
        assertFalse(DateUtils.isValidDate("2026-13-02"));
        assertEquals(
                LocalDate.of(2026, 1, 2)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                DateUtils.convertToTimestampFromDate("2026-01-02"));
        long timestamp = DateUtils.convertToTimeStampFromDateTime("2026-01-02 03:04:05");
        assertEquals("2026-01-02 03:04:05", DateUtils.convertToDateTimeFromTimestamp(timestamp));
        assertThrows(DateTimeException.class, () -> DateUtils.convertToTimestampFromDate("invalid"));
        assertThrows(DateTimeException.class, () -> DateUtils.convertToTimestampFromDate("2026-02-31"));
        assertThrows(DateTimeException.class, () -> DateUtils.convertToTimeStampFromDateTime("invalid"));
        assertFalse(DateUtils.judgeExceedTimeDuration(10L, 20L, 10L));
        assertTrue(DateUtils.judgeExceedTimeDuration(10L, 21L, 10L));
        assertThrows(IllegalArgumentException.class, () -> DateUtils.judgeExceedTimeDuration(20L, 10L, 10L));
        assertEquals(Long.valueOf(24), DateUtils.convertToHourFromTimeStamp(DateUtils.ONE_DAY_TIMESTAMP));
    }

    @Test
    void urlBuilderRetainsRepeatedValuesAndNullQueryParameters() {
        assertEquals("http://localhost/query", UrlUtils.buildUrl("http://localhost", "/query", null, null));
        assertEquals("http://localhost/query", UrlUtils.buildUrl("http://localhost", "/query", Map.of(), Map.of()));
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("list", List.of("a", "b"));
        values.put("array", new String[] {"c", "d"});
        values.put("number", 1);
        values.put("empty", null);
        assertEquals(
                "http://localhost/query?q=test&list=a&list=b&array=c&array=d&number=1&empty",
                UrlUtils.buildUrl("http://localhost", "/query", Map.of("q", "test"), values));
    }

    @Test
    void queryConversionFiltersNullMapEntriesAndHandlesConversionFailure() {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(Map.of(), UrlUtils.objectToQueryParamMap(null, mapper));
        Map<Object, Object> values = new HashMap<>();
        values.put(42, "value");
        values.put(null, "skip");
        values.put("skip", null);
        assertEquals(Map.of("42", "value"), UrlUtils.objectToQueryParamMap(values, mapper));
        NameSpaceDetail namespace = new NameSpaceDetail();
        namespace.setNamespace("public");
        assertEquals("public", UrlUtils.objectToQueryParamMap(namespace, mapper).get("namespace"));
        ObjectMapper failing = org.mockito.Mockito.mock(ObjectMapper.class);
        org.mockito.Mockito.when(failing.convertValue(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(tools.jackson.core.type.TypeReference.class)))
                .thenThrow(new IllegalArgumentException("invalid"));
        assertEquals(Map.of(), UrlUtils.objectToQueryParamMap("not-an-object", failing));
    }
}
