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
package io.seata.core.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The ExceptionEvent Test
 *
 * @author zhongxiang.wang
 */
public class ExceptionEventTest {
    @Test
    public void testGetName() {
        // Create an ExceptionEvent with a code
        ExceptionEvent exceptionEvent = new ExceptionEvent("CODE123");

        // Test the getName method
        assertEquals("CODE123", exceptionEvent.getName());
    }
}
