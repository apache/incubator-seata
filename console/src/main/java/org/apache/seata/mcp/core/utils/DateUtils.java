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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class DateUtils {

    public static final Long ONE_DAY_TIMESTAMP = 86400000L;

    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$");

    public static boolean isValidDate(String dateStr) {
        return DATE_PATTERN.matcher(dateStr).matches();
    }

    public static long convertToTimestampFromDate(String dateStr) {
        if (!isValidDate(dateStr)) {
            throw new DateTimeException("The time format does not match yyyy-MM-dd");
        }
        LocalDate date = LocalDate.parse(dateStr);
        ZonedDateTime zonedDateTime = date.atStartOfDay(ZoneId.systemDefault());
        return zonedDateTime.toInstant().toEpochMilli();
    }

    public static long convertToTimeStampFromDateTime(String dateTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, formatter);
            return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            throw new DateTimeException("The time format does not match yyyy-MM-dd HH:mm:ss", e);
        }
    }

    public static String convertToDateTimeFromTimestamp(Long timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime;
        try {
            dateTime = Instant.ofEpochMilli(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (DateTimeException | ArithmeticException e) {
            return "Parse Failed, please check that the timestamp is correct";
        }
        return dateTime.format(formatter);
    }

    public static boolean judgeExceedTimeDuration(Long startTime, Long endTime, Long maxDuration) {
        if (endTime < startTime) throw new IllegalArgumentException("endTime must not be earlier than startTime");
        return endTime - startTime > maxDuration;
    }

    public static Long convertToHourFromTimeStamp(Long timestamp) {
        return timestamp / (60 * 60 * 1000);
    }
}
