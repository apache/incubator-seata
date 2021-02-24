package io.seata.common.util;

import java.util.Calendar;
import java.util.Date;

/**
 * The datetime utils
 *
 * @author cebbank
 */
public class DateUtils {

    /**
     * a few days ago
     *
     * @param day day
     * @return date
     */
    public static Date getDayBefore(int day) {
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        now.set(Calendar.DATE, now.get(Calendar.DATE) - day);
        return now.getTime();
    }

    /**
     * a few hours ago
     * @param hour hour
     * @return date
     */
    public static Date getHourBefore(int hour) {
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        now.set(Calendar.HOUR, now.get(Calendar.HOUR) - hour);
        return now.getTime();
    }

    /**
     * a few minute ago
     * @param minute
     * @return
     */
    public static Date getMinuteBefore(int minute) {
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        now.set(Calendar.MINUTE, now.get(Calendar.MINUTE) - minute);
        return now.getTime();
    }

    /**
     * a few seconds ago
     * @param second second
     * @return
     */
    public static Date getSecondBefore(int second) {
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        now.set(Calendar.SECOND, now.get(Calendar.SECOND) - second);
        return now.getTime();
    }

}
