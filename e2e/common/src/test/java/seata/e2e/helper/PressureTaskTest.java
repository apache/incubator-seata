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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jingliu_xiong@foxmail.com
 */
public class PressureTaskTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PressureTaskTest.class);

    @Test
    public void pressureTest(){
        PressureTask pressureTask = new PressureTask(() -> {
            System.out.println("Task is Running.");
            return "one task is over";
            }, 100, 20);
        pressureTask.start(false);
        System.out.println("The main function is over");
    }

    @Test
    public void pressureTestWithJudge(){
        PressureTask pressureTask = new PressureTask(() -> {
            System.out.println("Task is Running.");
            return "one task is over";
        }, 100, 20);
        pressureTask.start(false, r -> {
            String res = (String) r;
            if (res.equals("one task is over")) {
                return true;
            } else {
                return false;
            }
        });
        System.out.println("The main function is over");
    }

}