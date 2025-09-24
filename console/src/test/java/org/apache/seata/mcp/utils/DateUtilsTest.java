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
package org.apache.seata.mcp.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DateUtilsTest {

    @Test
    @DisplayName("Test valid date formats")
    public void testIsValidDate_ValidFormats() {
        assertTrue(DateUtils.isValidDate("2023-01-01"));
        assertTrue(DateUtils.isValidDate("2023-12-31"));
        assertTrue(DateUtils.isValidDate("2024-02-29")); // leap year
        assertTrue(DateUtils.isValidDate("2023-06-15"));
    }

    @ParameterizedTest
    @DisplayName("Test invalid date formats")
    @ValueSource(
            strings = {
                "2023-1-1", // month and day not two digits
                "23-01-01", // year not four digits
                "2023/01/01", // wrong separator
                "2023-13-01", // month out of range
                "2023-01-32", // day out of range
                "2023-00-01", // month is 0
                "2023-01-00", // day is 0
                "", // empty string
                "abc-def-ghi", // non-numeric
                "2023-01", // incomplete format
                "2023-01-01 10:30:00" // includes time part
            })
    public void testIsValidDate_InvalidFormats(String invalidDate) {
        assertFalse(DateUtils.isValidDate(invalidDate));
    }

    @Test
    @DisplayName("Test converting date to timestamp")
    public void testConvertToTimestampFromDate_ValidDate() {
        // Use fixed date for testing
        String dateStr = "2023-06-15";
        long timestamp = DateUtils.convertToTimestampFromDate(dateStr);

        // Verify timestamp is not negative
        assertTrue(timestamp > 0);

        // Verify conversion result is start of day
        LocalDate expectedDate = LocalDate.parse(dateStr);
        ZonedDateTime expectedZonedDateTime = expectedDate.atStartOfDay(ZoneId.systemDefault());
        long expectedTimestamp = expectedZonedDateTime.toInstant().toEpochMilli();

        assertEquals(expectedTimestamp, timestamp);
    }

    @Test
    @DisplayName("Test invalid date conversion throws exception")
    public void testConvertToTimestampFromDate_InvalidDate() {
        assertThrows(DateTimeException.class, () -> {
            DateUtils.convertToTimestampFromDate("2023-13-01");
        });

        assertThrows(DateTimeException.class, () -> {
            DateUtils.convertToTimestampFromDate("invalid-date");
        });
    }

    @Test
    @DisplayName("Test converting datetime to timestamp")
    public void testConvertToTimeStampFromDateTime_ValidDateTime() {
        String dateTimeStr = "2023-06-15 14:30:45";
        long timestamp = DateUtils.convertToTimeStampFromDateTime(dateTimeStr);

        // Verify timestamp is not negative
        assertTrue(timestamp > 0);

        // Verify conversion accuracy
        LocalDateTime expectedDateTime =
                LocalDateTime.parse(dateTimeStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        long expectedTimestamp =
                expectedDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        assertEquals(expectedTimestamp, timestamp);
    }

    @ParameterizedTest
    @DisplayName("Test invalid datetime conversion returns -1")
    @ValueSource(
            strings = {
                "2023-06-15 25:30:45", // invalid hour
                "2023-06-15 14:60:45", // invalid minute
                "2023-06-15 14:30:61", // invalid second
                "2023-13-15 14:30:45", // invalid month
                "2023-06-32 14:30:45", // invalid day
                "invalid-datetime", // completely invalid format
                "2023-06-15", // missing time part
                "", // empty string
                "2023/06/15 14:30:45" // wrong date separator
            })
    public void testConvertToTimeStampFromDateTime_InvalidDateTime(String invalidDateTime) {
        assertEquals(-1, DateUtils.convertToTimeStampFromDateTime(invalidDateTime));
    }

    @Test
    @DisplayName("Test converting timestamp to datetime")
    public void testConvertToDateTimeFromTimestamp_ValidTimestamp() {
        // Use current timestamp for testing
        long timestamp = System.currentTimeMillis();
        String dateTimeStr = DateUtils.convertToDateTimeFromTimestamp(timestamp);

        // Verify returned string is not null and not empty
        assertNotNull(dateTimeStr);
        assertFalse(dateTimeStr.isEmpty());

        // Verify format is correct
        assertTrue(dateTimeStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));

        // Verify conversion accuracy
        LocalDateTime expectedDateTime =
                Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
        String expectedStr =
                expectedDateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        assertEquals(expectedStr, dateTimeStr);
    }

    @Test
    @DisplayName("Test special timestamp conversion")
    public void testConvertToDateTimeFromTimestamp_SpecialTimestamps() {
        // Test zero timestamp (1970-01-01 00:00:00 UTC)
        String result = DateUtils.convertToDateTimeFromTimestamp(0L);
        assertNotNull(result);
        assertTrue(result.startsWith("1970-01-01"));

        // Test positive timestamp
        String result2 = DateUtils.convertToDateTimeFromTimestamp(1687000000000L); // around 2023
        assertNotNull(result2);
        assertTrue(result2.startsWith("2023"));
    }

    @Test
    @DisplayName("Test judging if time duration exceeds limit")
    public void testJudgeExceedTimeDuration() {
        long startTime = 1000L;
        long endTime = 5000L;
        long maxDuration = 3000L;

        // Case where duration exceeds maximum
        assertTrue(DateUtils.judgeExceedTimeDuration(startTime, endTime, maxDuration));

        // Case where duration does not exceed maximum
        long maxDuration2 = 5000L;
        assertFalse(DateUtils.judgeExceedTimeDuration(startTime, endTime, maxDuration2));

        // Case where duration equals maximum
        long maxDuration3 = 4000L;
        assertFalse(DateUtils.judgeExceedTimeDuration(startTime, endTime, maxDuration3));
    }

    @ParameterizedTest
    @DisplayName("Test different duration scenarios")
    @CsvSource({
        "1000, 2000, 500, true", // exceeds
        "1000, 2000, 1000, false", // equals
        "1000, 2000, 1500, false", // does not exceed
        "0, 1000, 999, true", // exceeds
        "0, 1000, 1000, false", // equals
        "5000, 3000, 1000, false" // end time less than start time
    })
    public void testJudgeExceedTimeDuration_ParameterizedTest(
            long startTime, long endTime, long maxDuration, boolean expected) {
        assertEquals(expected, DateUtils.judgeExceedTimeDuration(startTime, endTime, maxDuration));
    }

    @Test
    @DisplayName("Test boundary values")
    public void testBoundaryValues() {
        // Test minimum and maximum valid dates
        assertTrue(DateUtils.isValidDate("0001-01-01"));
        assertTrue(DateUtils.isValidDate("9999-12-31"));

        // Test leap years
        assertTrue(DateUtils.isValidDate("2000-02-29")); // leap year
        assertTrue(DateUtils.isValidDate("2004-02-29")); // leap year
    }

    @Test
    @DisplayName("Test null value handling")
    public void testNullHandling() {
        // isValidDate null handling
        assertThrows(NullPointerException.class, () -> {
            DateUtils.isValidDate(null);
        });

        // convertToTimestampFromDate null handling
        assertThrows(NullPointerException.class, () -> {
            DateUtils.convertToTimestampFromDate(null);
        });

        // convertToTimeStampFromDateTime null handling
        assertEquals(-1, DateUtils.convertToTimeStampFromDateTime(null));

        // convertToDateTimeFromTimestamp null handling
        assertThrows(NullPointerException.class, () -> {
            DateUtils.convertToDateTimeFromTimestamp(null);
        });
    }
}
