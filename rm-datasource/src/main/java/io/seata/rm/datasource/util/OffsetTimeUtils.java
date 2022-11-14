/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.rm.datasource.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Short.toUnsignedInt;
import static java.time.ZoneOffset.UTC;

/**
 * Currently, common time zones are supported
 *
 * @see java.time.ZoneId
 *
 *
 * @author doubleDimple
 */
public class OffsetTimeUtils {

    public static final String PATTERN_FORMAT_TIME = "yyyy-MM-dd HH:mm:ss.SSS";

    private static final byte REGIONIDBIT = (byte)0b1000_0000;

    private static final Map<Integer, String> ZONE_ID_MAP = new HashMap<>(36);

    public static String getRegion(int code) {
        return ZONE_ID_MAP.get(code);
    }

    public static String convertOffSetTime(OffsetDateTime offsetDateTime) {
        if (null != offsetDateTime) {
            return offsetDateTime.format(DateTimeFormatter.ofPattern(PATTERN_FORMAT_TIME));
        }
        return null;
    }

    public static OffsetDateTime timeToOffsetDateTime(byte[] bytes) {
        if (null == bytes || bytes.length == 0) {
            return null;
        }

        OffsetDateTime utc = extractUtc(bytes);
        if (bytes.length >= 8) {
            if (isFixedOffset(bytes)) {
                ZoneOffset offset = extractOffset(bytes);
                return utc.withOffsetSameInstant(offset);
            } else {
                ZoneId zoneId = extractZoneId(bytes);
                return utc.atZoneSameInstant(zoneId).toOffsetDateTime();
            }
        }
        return utc.atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private static boolean isFixedOffset(byte[] bytes) {
        return (bytes[11] & REGIONIDBIT) == 0;
    }

    private static OffsetDateTime extractUtc(byte[] bytes) {
        return OffsetDateTime.of(extractLocalDateTime(bytes), UTC);
    }

    private static ZoneId extractZoneId(byte[] bytes) {
        // high order bits
        int regionCode = (bytes[11] & 0b1111111) << 6;
        // low order bits
        regionCode += (bytes[12] & 0b11111100) >> 2;
        String regionName = getRegion(regionCode);
        return ZoneId.of(regionName);
    }

    private static ZoneOffset extractOffset(byte[] bytes) {
        int hours = bytes[11] - 20;
        int minutes = bytes[12] - 60;
        if ((hours == 0) && (minutes == 0)) {
            return ZoneOffset.UTC;
        }
        return ZoneOffset.ofHoursMinutes(hours, minutes);
    }

    private static LocalDateTime extractLocalDateTime(byte[] bytes) {
        int year = ((toUnsignedInt(bytes[0]) - 100) * 100) + (toUnsignedInt(bytes[1]) - 100);
        int month = bytes[2];
        int dayOfMonth = bytes[3];
        int hour = bytes[4] - 1;
        int minute = bytes[5] - 1;
        int second = bytes[6] - 1;
        int nanoOfSecond = 0;
        if (bytes.length >= 8) {
            nanoOfSecond = (toUnsignedInt(bytes[7]) << 24) | (toUnsignedInt(bytes[8]) << 16)
                | (toUnsignedInt(bytes[9]) << 8) | toUnsignedInt(bytes[10]);
        }
        return LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond);
    }

    static {
        ZONE_ID_MAP.put(250, "Asia/Shanghai");
        ZONE_ID_MAP.put(345, "Australia/Darwin");
        ZONE_ID_MAP.put(352, "Australia/Sydney");
        ZONE_ID_MAP.put(687, "America/Argentina/Buenos_Aires");
        ZONE_ID_MAP.put(106, "America/Anchorage");
        ZONE_ID_MAP.put(188, "America/Sao_Paulo");
        ZONE_ID_MAP.put(756, "Asia/Dhaka");
        ZONE_ID_MAP.put(80, "Africa/Harare");
        ZONE_ID_MAP.put(118, "America/St_Johns");
        ZONE_ID_MAP.put(101, "America/Chicago");
        ZONE_ID_MAP.put(47, "Africa/Addis_Ababa");
        ZONE_ID_MAP.put(382, "Europe/Paris");
        ZONE_ID_MAP.put(1647, "America/Indiana/Indianapolis");
        ZONE_ID_MAP.put(772, "Asia/Kolkata");
        ZONE_ID_MAP.put(267, "Asia/Tokyo");
        ZONE_ID_MAP.put(479, "Pacific/Apia");
        ZONE_ID_MAP.put(241, "Asia/Yerevan");
        ZONE_ID_MAP.put(471, "Pacific/Auckland");
        ZONE_ID_MAP.put(284, "Asia/Karachi");
        ZONE_ID_MAP.put(109, "America/Phoenix");
        ZONE_ID_MAP.put(167, "America/Puerto_Rico");
        ZONE_ID_MAP.put(103, "America/Los_Angeles");
        ZONE_ID_MAP.put(481, "Pacific/Guadalcanal");
        ZONE_ID_MAP.put(813, "Asia/Ho_Chi_Minh");
        ZONE_ID_MAP.put(1474, "HST");
        ZONE_ID_MAP.put(1636, "EST");
        ZONE_ID_MAP.put(2662, "MST");
    }
}
