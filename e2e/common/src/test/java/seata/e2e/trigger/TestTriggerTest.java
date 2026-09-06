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

package seata.e2e.trigger;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jingliu_xiong@foxmail.com
 */
public class TestTriggerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestTriggerTest.class);

    private static final AtomicInteger TIMES1 = new AtomicInteger(1);
    private static final AtomicInteger TIMES2 = new AtomicInteger(1);


    @Timeout(10)
    @TestTrigger
    @DisplayName("should retry on any failure")
    void shouldRetryOnAnyFailure() {
        while (true) {
            if (TIMES1.getAndIncrement() == 10) {
                LOGGER.info("test passed");
                break;
            }
        }
    }

    @Timeout(10)
    @TestTrigger(value = 2, throwables = {ArithmeticException.class, RuntimeException.class}, interval = 10000)
    void shouldRetryOnSpecficExceptionAndOneTime() {
        int i = 1 / 0;
    }


    @Timeout(10)
    @TestTrigger(3)
    @DisplayName("should retry specific times")
    void shouldRetrySpecificTimes() {
        if (TIMES2.getAndIncrement() < 3) {
            LOGGER.info("test passed");
        }
    }

    @Timeout(10)
    @TestTrigger(1)
    @DisplayName("should retry before time out")
    void shouldRetryByTimeout() {
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}