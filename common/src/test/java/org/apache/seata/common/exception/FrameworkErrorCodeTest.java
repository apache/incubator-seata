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
package org.apache.seata.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The type Framework error code test.
 */
public class FrameworkErrorCodeTest {

    @Test
    public void testGetErrCode() {
        assertEquals("0004", FrameworkErrorCode.ThreadPoolFull.getErrCode());
        assertEquals("0101", FrameworkErrorCode.NetConnect.getErrCode());
        assertEquals("10000", FrameworkErrorCode.UnknownAppError.getErrCode());
    }

    @Test
    public void testGetErrMessage() {
        assertEquals("Thread pool is full", FrameworkErrorCode.ThreadPoolFull.getErrMessage());
        assertEquals("Can not connect to the server", FrameworkErrorCode.NetConnect.getErrMessage());
        assertEquals("Unknown error", FrameworkErrorCode.UnknownAppError.getErrMessage());
    }

    @Test
    public void testGetErrDispose() {
        assertEquals("Please check the thread pool configuration", FrameworkErrorCode.ThreadPoolFull.getErrDispose());
        assertEquals(
                "Please check if the seata service is started. Is the network connection to the seata server normal?",
                FrameworkErrorCode.NetConnect.getErrDispose());
        assertEquals("Internal error", FrameworkErrorCode.UnknownAppError.getErrDispose());
    }

    @Test
    public void testToString() {
        String expected = "[0004] [Thread pool is full] [Please check the thread pool configuration]";
        assertEquals(expected, FrameworkErrorCode.ThreadPoolFull.toString());

        String expected2 = "[10000] [Unknown error] [Internal error]";
        assertEquals(expected2, FrameworkErrorCode.UnknownAppError.toString());
    }

    @Test
    public void testAllErrorCodesHaveValues() {
        for (FrameworkErrorCode errorCode : FrameworkErrorCode.values()) {
            assertNotNull(errorCode.getErrCode(), "Error code should not be null for " + errorCode);
            assertNotNull(errorCode.getErrMessage(), "Error message should not be null for " + errorCode);
            assertNotNull(errorCode.getErrDispose(), "Error dispose should not be null for " + errorCode);
        }
    }
}
