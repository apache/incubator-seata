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

import org.apache.seata.common.util.ReflectionUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;

class OffsetTimeUtilsTest {

    // 2026-01-28 15:30:45.000
    private final OffsetDateTime MOCK_UTC_DATE = OffsetDateTime.of(2026, 1, 28, 15, 30, 45, 0, ZoneOffset.UTC);

    @ParameterizedTest
    @MethodSource("getZoneIds")
    public void shouldReturnRegionGivenCode(int code, String expectedRegion) {
        assertEquals(expectedRegion, OffsetTimeUtils.getRegion(code));
    }

    @Test
    public void shouldReturnNullWhenGetRegionByInvalidCode() {
        assertNull(OffsetTimeUtils.getRegion(-1));
        assertNull(OffsetTimeUtils.getRegion(100));
    }

    @Test
    public void shouldReturnNullWhenConvertNullOffsetDateTime() {
        assertNull(OffsetTimeUtils.convertOffSetTime(null));
    }

    @Test
    public void shouldConvertOffsetDateTimeToString() {
        String expected = "2026-01-28 15:30:45.000";
        assertEquals(expected, OffsetTimeUtils.convertOffSetTime(MOCK_UTC_DATE));
    }

    @Test
    public void shouldReturnNullWhenConvertNullByteArray() {
        assertNull(OffsetTimeUtils.timeToOffsetDateTime(null));
        assertNull(OffsetTimeUtils.timeToOffsetDateTime(new byte[0]));
    }

    @Test
    void shouldUseSystemDefaultWhenBytesLessThan8() {
        ZoneId mockZone = ZoneId.of("UTC");
        byte[] bytes = new byte[7];
        // year = 2026 -> (120 - 100)*100 + (126 - 100) = 2026
        bytes[0] = (byte) 120;
        bytes[1] = (byte) 126;
        bytes[2] = (byte) 1; // month = 1
        bytes[3] = (byte) 28; // day = 28
        bytes[4] = (byte) 16; // hour = 16 - 1 = 15
        bytes[5] = (byte) 31; // minute = 31 - 1 = 30
        bytes[6] = (byte) 46; // second = 46 - 1 = 45

        try (MockedStatic<ZoneId> mockedZoneId = mockStatic(ZoneId.class)) {
            mockedZoneId.when(ZoneId::systemDefault).thenReturn(mockZone);

            OffsetDateTime expected = MOCK_UTC_DATE.atZoneSameInstant(mockZone).toOffsetDateTime();

            OffsetDateTime actual = OffsetTimeUtils.timeToOffsetDateTime(bytes);

            assertEquals(expected, actual);
        }
    }

    @Test
    void shouldUseFixedOffsetWhenBytesLengthGreaterOrEqual8() {
        byte[] bytes = new byte[13];
        // same date/time encoding as above
        bytes[0] = (byte) 120;
        bytes[1] = (byte) 126;
        bytes[2] = (byte) 1;
        bytes[3] = (byte) 28;
        bytes[4] = (byte) 16;
        bytes[5] = (byte) 31;
        bytes[6] = (byte) 46;
        // nanos (bytes[7..10]) = 0
        bytes[7] = 0;
        bytes[8] = 0;
        bytes[9] = 0;
        bytes[10] = 0;
        // bytes[11] with top bit 0 => fixed offset. hours = bytes[11] - 20
        // to get +8 hours -> bytes[11] = 8 + 20 = 28
        bytes[11] = (byte) 28;
        // minutes = bytes[12] - 60; to get 0 minutes -> bytes[12] = 60
        bytes[12] = (byte) 60;

        OffsetDateTime expected =
                MOCK_UTC_DATE.atZoneSameInstant(ZoneId.of("Asia/Shanghai")).toOffsetDateTime();

        OffsetDateTime actual = OffsetTimeUtils.timeToOffsetDateTime(bytes);

        assertEquals(expected, actual);
    }

    @Test
    void shouldUseZoneIdWhenRegionBitSet() {
        byte[] bytes = new byte[13];
        // date/time encoding
        bytes[0] = (byte) 120;
        bytes[1] = (byte) 126;
        bytes[2] = (byte) 1;
        bytes[3] = (byte) 28;
        bytes[4] = (byte) 16;
        bytes[5] = (byte) 31;
        bytes[6] = (byte) 46;
        // nanos bytes[7..10] = 0
        bytes[7] = 0;
        bytes[8] = 0;
        bytes[9] = 0;
        bytes[10] = 0;
        // bytes[11] top bit = 1 to indicate region id; lower 7 bits = 3 (so high part = 3)
        bytes[11] = (byte) 0x83; // 0b1000_0011
        // bytes[12] masked with 0b11111100 should yield 232 -> low part = 58
        bytes[12] = (byte) 232; // (232 & 0b11111100) >> 2 == 58

        OffsetDateTime expected =
                MOCK_UTC_DATE.atZoneSameInstant(ZoneId.of("Asia/Shanghai")).toOffsetDateTime();

        OffsetDateTime actual = OffsetTimeUtils.timeToOffsetDateTime(bytes);

        assertEquals(expected, actual);
    }

    @Test
    void shouldReturnUTCWhenOffsetHoursAndMinutesAreZero() {
        byte[] bytes = new byte[13];
        // date/time encoding -> 2026-01-28 15:30:45 (internal encoding)
        bytes[0] = (byte) 120;
        bytes[1] = (byte) 126;
        bytes[2] = (byte) 1;
        bytes[3] = (byte) 28;
        bytes[4] = (byte) 16; // hour = 16 - 1 = 15
        bytes[5] = (byte) 31; // minute = 31 - 1 = 30
        bytes[6] = (byte) 46; // second = 46 - 1 = 45
        // nanos bytes[7..10] = 0
        bytes[7] = 0;
        bytes[8] = 0;
        bytes[9] = 0;
        bytes[10] = 0;
        // set fixed offset with hours == 0 and minutes == 0
        bytes[11] = (byte) 20; // hours = 20 - 20 = 0 (top bit 0 => fixed offset)
        bytes[12] = (byte) 60; // minutes = 60 - 60 = 0

        OffsetDateTime actual = OffsetTimeUtils.timeToOffsetDateTime(bytes);

        assertEquals(MOCK_UTC_DATE, actual);
    }

    @SuppressWarnings("InstantiationOfUtilityClass")
    private static Stream<Arguments> getZoneIds() throws NoSuchFieldException {
        Map<Integer, String> zoneIdMap = ReflectionUtil.getFieldValue(new OffsetTimeUtils(), "ZONE_ID_MAP");
        return zoneIdMap.entrySet().stream().map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
    }
}
