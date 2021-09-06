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

package com.demo.util;

import com.demo.helper.PressureTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xjl
 * @Description: 一个用来统计一共花费多少时间的工具类
 */
public class TimeCountUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeCountUtil.class);

    private static long startTime = 0;
    private static long totalTime = 0;

    public static void startTimeCount() {
        startTime = System.currentTimeMillis();
        totalTime += startTime;
    }

    public static void pauseTimeCount() {
        long tempTime = System.currentTimeMillis();
        totalTime +=  tempTime - startTime;
        startTime = tempTime;
    }

    public static void stopTimeCount() {
        long tempTime = System.currentTimeMillis();
        totalTime +=  tempTime - startTime;
        LOGGER.info("Total run time: {} ms", totalTime);
        startTime = 0;
        totalTime = 0;
    }
}
