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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * The type Date util.
 *
 * @author slievrly
 */
public class DateUtil {

    /**
     * Gets current date.
     *
     * @return the current date
     */
    public static Date getCurrentDate() {
        return new Date();
    }

    /**
     * Parse date date.
     *
     * @param dateStr the date str
     * @param format  the format
     * @return the date
     * @throws ParseException the parse exception
     */
    public static Date parseDate(String dateStr, String format) throws ParseException {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.parse(dateStr);
    }

    public static Date parseDateWithoutTime(String dateStr) throws ParseException {
        return parseDate(dateStr, "yyyy-MM-dd");
    }

    public static Date getDateNowPlusDays(int days) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, days);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = dateFormat.format(calendar.getTime());
        return dateFormat.parse(dateStr);
    }

    /**
     * Format date string.
     *
     * @param date   the date
     * @param format the format
     * @return the string
     */
    public static String formatDate(Date date, String format) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }
}
