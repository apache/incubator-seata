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

package seata.e2e.helper;

import org.junit.jupiter.api.Test;

/**
 * @author jingliu_xiong@foxmail.com
 */
public class TimesTaskTest {

    @Test
    public void timesTest(){
        int times = 3;
        int interval = 1000;
        TimesTask timesTask = new TimesTask(() -> {
            System.out.println("Time task is running!");
            return "one task is over";
        }, times, interval);
        timesTask.start();
        System.out.println("The main function is over");
    }

    @Test
    public void timesTest1(){
        int times = 3;
        int interval = 1000;
        TimesTask timesTask = new TimesTask(() -> {
            int i = 1/0;
            return "one task is over";
        }, times, interval);
        timesTask.start();
        System.out.println("The main function is over");
    }
}