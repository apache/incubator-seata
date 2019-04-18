package io.seata.common.util;

import java.time.Duration;

/**
 * @author XCXCXCXCX
 */
public class DurationUtil {

    public static final String DAY_UNIT = "d";
    public static final String HOUR_UNIT = "h";
    public static final String MINIUTE_UNIT = "m";
    public static final String SECOND_UNIT = "s";
    public static final String MILLIS_SECOND_UNIT = "ms";

    public static Duration parse(String str) {
        if (str.contains(MILLIS_SECOND_UNIT)) {
            Long value = doParse(MILLIS_SECOND_UNIT, str);
            return value == null ? null : Duration.ofMillis(value);
        } else if (str.contains(DAY_UNIT)) {
            Long value = doParse(DAY_UNIT, str);
            return value == null ? null : Duration.ofDays(value);
        } else if (str.contains(HOUR_UNIT)) {
            Long value = doParse(HOUR_UNIT, str);
            return value == null ? null : Duration.ofHours(value);
        } else if (str.contains(MINIUTE_UNIT)) {
            Long value = doParse(MINIUTE_UNIT, str);
            return value == null ? null : Duration.ofMinutes(value);
        } else if (str.contains(SECOND_UNIT)) {
            Long value = doParse(SECOND_UNIT, str);
            return value == null ? null : Duration.ofSeconds(value);
        }
        throw new UnsupportedOperationException(str + " can't parse to duration");
    }

    private static Long doParse(String unit, String str) {
        str = str.replace(unit, "");
        if ("".equals(str)) {
            return null;
        }
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            throw new UnsupportedOperationException("\"" + str + "\" can't parse to Duration", e);
        }
    }

    public static void main(String[] args) {
        System.out.println(parse("1d").getSeconds());
        System.out.println(parse("1h").getSeconds());
        System.out.println(parse("1m").getSeconds());
        System.out.println(parse("1s").getSeconds());
        System.out.println(parse("1ms").getSeconds());
        System.out.println(parse("-1ms").getSeconds());
    }

}
