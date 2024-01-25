/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.core.protocol;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link ResultCode}.
 *
 */
class ResultCodeTest {

    @Test
    void getByte() {
        Assertions.assertEquals(ResultCode.Failed, ResultCode.get((byte) 0));
        Assertions.assertEquals(ResultCode.Success, ResultCode.get((byte) 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ResultCode.get((byte) 2);
        });
    }

    @Test
    void getInt() {
        Assertions.assertEquals(ResultCode.Failed, ResultCode.get(0));
        Assertions.assertEquals(ResultCode.Success, ResultCode.get(1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ResultCode.get(2);
        });
    }

    @Test
    void values() {
        Assertions.assertArrayEquals(new ResultCode[]{ResultCode.Failed, ResultCode.Success}, ResultCode.values());
    }

    @Test
    void valueOf() {
        Assertions.assertEquals(ResultCode.Failed, ResultCode.valueOf("Failed"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ResultCode.valueOf("FAILED");
        });
        Assertions.assertEquals(ResultCode.Success, ResultCode.valueOf("Success"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ResultCode.valueOf("SUCCESS");
        });
    }
}
