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
package org.apache.seata.saga.statelang.domain.impl;

import org.apache.seata.saga.statelang.domain.StateType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for FailEndStateImpl
 */
public class FailEndStateImplTest {

    @Test
    public void testFailEndStateImpl() {
        // Test constructor sets correct type
        FailEndStateImpl failEndState = new FailEndStateImpl();
        Assertions.assertEquals(StateType.FAIL, failEndState.getType());

        // Test error code
        String errorCode = "ERROR_500";
        failEndState.setErrorCode(errorCode);
        Assertions.assertEquals(errorCode, failEndState.getErrorCode());

        // Test message
        String message = "Operation failed";
        failEndState.setMessage(message);
        Assertions.assertEquals(message, failEndState.getMessage());
    }
}
