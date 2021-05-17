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
package io.seata.common.util;

import java.util.Calendar;
import java.util.Date;

/**
 * The datetime utils
 *
 * @author kaka2code
 */
public class DateUtils {

    /**
     * few days ago
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
     * few hours ago
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
     * few minute ago
     * @param minute minute
     * @return date
     */
    public static Date getMinuteBefore(int minute) {
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        now.set(Calendar.MINUTE, now.get(Calendar.MINUTE) - minute);
        return now.getTime();
    }

    /**
     * few seconds ago
     * @param second second
     * @return date
     */
    public static Date getSecondBefore(int second) {
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        now.set(Calendar.SECOND, now.get(Calendar.SECOND) - second);
        return now.getTime();
    }

}
