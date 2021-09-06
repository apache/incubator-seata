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

package helper;

import com.demo.helper.PressureTask;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xjl
 * @Description:
 */
public class PressureTaskTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PressureTaskTest.class);

    @Test
    public void timesTest(){
        PressureTask pressureTask = new PressureTask(() -> {
            System.out.println("Hello Lambda!");
            return "one task is over";
            }, 100, 20);
        pressureTask.start();
        System.out.println("The main function is over");
        try {
            Thread.sleep(10000000000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}