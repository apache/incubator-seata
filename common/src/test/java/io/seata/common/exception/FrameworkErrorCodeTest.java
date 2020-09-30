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
package io.seata.common.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.seata.common.exception.FrameworkErrorCode.ThreadPoolFull;

/**
 * The test for {@link FrameworkErrorCode}
 *
 * @author wang.liang
 */
public class FrameworkErrorCodeTest {

    @Test
    void test_getErrCode() {
        Assertions.assertEquals(ThreadPoolFull.getErrCode(), "0004");
    }

    @Test
    void test_getErrMessage() {
        Assertions.assertEquals(ThreadPoolFull.getErrMessage(), "Thread pool is full");
    }

    @Test
    void test_getErrDispose() {
        Assertions.assertEquals(ThreadPoolFull.getErrDispose(), "Please check the thread pool configuration");
    }

    @Test
    void test_toString() {
        Assertions.assertEquals(ThreadPoolFull.toString(),
                String.format("[%s] [%s] [%s]", ThreadPoolFull.getErrCode(), ThreadPoolFull.getErrMessage(), ThreadPoolFull.getErrDispose()));
    }
}
