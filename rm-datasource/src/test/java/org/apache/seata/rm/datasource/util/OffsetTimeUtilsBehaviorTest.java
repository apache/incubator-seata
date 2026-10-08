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
package org.apache.seata.rm.datasource.util;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class OffsetTimeUtilsBehaviorTest {
    @Test
    void absentValuesAndFormatting() {
        assertNull(OffsetTimeUtils.convertOffSetTime(null));
        assertNull(OffsetTimeUtils.timeToOffsetDateTime(null));
        assertNull(OffsetTimeUtils.timeToOffsetDateTime(new byte[0]));
        assertEquals(
                "2024-03-02 12:34:56.123",
                OffsetTimeUtils.convertOffSetTime(OffsetDateTime.parse("2024-03-02T12:34:56.123456789+08:00")));
        assertEquals("Asia/Shanghai", OffsetTimeUtils.getRegion(250));
        assertNull(OffsetTimeUtils.getRegion(-1));
    }

    @Test
    void oracleTimestampWithoutZoneUsesSystemOffset() {
        byte[] bytes = {120, 124, 3, 2, 13, 35, 57};
        OffsetDateTime utc = OffsetDateTime.parse("2024-03-02T12:34:56Z");
        assertEquals(
                utc.atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime(),
                OffsetTimeUtils.timeToOffsetDateTime(bytes));
    }

    @Test
    void fixedOffsetsPreserveInstantAndFractionalSeconds() {
        byte[] bytes = {120, 124, 3, 2, 13, 35, 57, 0, 0, 1, 0, 20, 60};
        OffsetDateTime utc = OffsetDateTime.parse("2024-03-02T12:34:56.000000256Z");
        assertEquals(utc, OffsetTimeUtils.timeToOffsetDateTime(bytes));
        bytes[11] = 25;
        bytes[12] = 90;
        assertEquals(
                utc.withOffsetSameInstant(ZoneOffset.ofHoursMinutes(5, 30)),
                OffsetTimeUtils.timeToOffsetDateTime(bytes));
        bytes[11] = 16;
        bytes[12] = 30;
        assertEquals(
                utc.withOffsetSameInstant(ZoneOffset.ofHoursMinutes(-4, -30)),
                OffsetTimeUtils.timeToOffsetDateTime(bytes));
    }

    @Test
    void regionOffsetHonorsDaylightSaving() {
        // Oracle region code 103 denotes America/Los_Angeles.
        byte[] bytes = {120, 124, 7, 2, 13, 35, 57, 0, 0, 0, 0, (byte) 129, (byte) 156};
        assertEquals(OffsetDateTime.parse("2024-07-02T05:34:56-07:00"), OffsetTimeUtils.timeToOffsetDateTime(bytes));
        bytes[2] = 1;
        assertEquals(OffsetDateTime.parse("2024-01-02T04:34:56-08:00"), OffsetTimeUtils.timeToOffsetDateTime(bytes));
    }
}
