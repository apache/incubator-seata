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
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author slievrly
 */
public class DateUtilTest {
    @Test
    public void testGetCurrentDate() {
        Date currentDate = DateUtil.getCurrentDate();
        Assertions.assertNotNull(currentDate);
    }

    @Test
    public void testParseDate() throws ParseException {
        String dateStr = "2021-01-01";
        Date date = DateUtil.parseDate(dateStr, "yyyy-MM-dd");
        Assertions.assertNotNull(date);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Assertions.assertEquals(dateStr, sdf.format(date));
    }

    @Test
    public void testFormatDate() {
        Date currentDate = DateUtil.getCurrentDate();
        String dateStr = DateUtil.formatDate(currentDate, "yyyy-MM-dd HH:mm:ss");
        Assertions.assertNotNull(dateStr);
    }

    @Test
    public void testGetDateNowPlusDays() throws ParseException {
        Assertions.assertNotNull(DateUtil.getDateNowPlusDays(2));
    }
}
