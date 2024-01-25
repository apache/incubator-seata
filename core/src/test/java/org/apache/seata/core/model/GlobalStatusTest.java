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
package org.apache.seata.core.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * A unit test for {@link GlobalStatus}
 *
 */
public class GlobalStatusTest {
    private static final int BEGIN_CODE = 1;
    private static final int NONE = 99;
    private static final int MIN_CODE = 0;
    private static final int MAX_CODE = 15;

    @Test
    public void testGetCode() {
        int code = GlobalStatus.Begin.getCode();
        Assertions.assertEquals(code, BEGIN_CODE);
    }

    @Test
    public void testGetWithByte() {
        GlobalStatus branchStatus = GlobalStatus.get((byte) BEGIN_CODE);
        Assertions.assertEquals(branchStatus, GlobalStatus.Begin);
    }

    @Test
    public void testGetWithInt() {
        GlobalStatus branchStatus = GlobalStatus.get(BEGIN_CODE);
        Assertions.assertEquals(branchStatus, GlobalStatus.Begin);
    }

    @Test
    public void testGetException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> GlobalStatus.get(NONE));
    }

    @Test
    public void testGetByCode() {
        GlobalStatus globalStatusOne = GlobalStatus.get(MIN_CODE);
        Assertions.assertEquals(globalStatusOne, GlobalStatus.UnKnown);

        GlobalStatus globalStatusTwo = GlobalStatus.get(MAX_CODE);
        Assertions.assertEquals(globalStatusTwo, GlobalStatus.Finished);

        Assertions.assertThrows(IllegalArgumentException.class, () -> GlobalStatus.get(NONE));
    }

    @Test
    public void testIsOnePhaseTimeout() {
        Assertions.assertFalse(GlobalStatus.isOnePhaseTimeout(GlobalStatus.Begin));
        Assertions.assertFalse(GlobalStatus.isOnePhaseTimeout(GlobalStatus.Rollbacking));
        Assertions.assertTrue(GlobalStatus.isOnePhaseTimeout(GlobalStatus.TimeoutRollbacking));
        Assertions.assertTrue(GlobalStatus.isOnePhaseTimeout(GlobalStatus.TimeoutRollbackRetrying));
        Assertions.assertTrue(GlobalStatus.isOnePhaseTimeout(GlobalStatus.TimeoutRollbacked));
        Assertions.assertTrue(GlobalStatus.isOnePhaseTimeout(GlobalStatus.TimeoutRollbackFailed));
    }

    @Test
    public void testIsTwoPhaseSuccess() {
        Assertions.assertTrue(GlobalStatus.isTwoPhaseSuccess(GlobalStatus.Committed));
        Assertions.assertTrue(GlobalStatus.isTwoPhaseSuccess(GlobalStatus.Rollbacked));
        Assertions.assertTrue(GlobalStatus.isTwoPhaseSuccess(GlobalStatus.TimeoutRollbacked));
        Assertions.assertFalse(GlobalStatus.isTwoPhaseSuccess(GlobalStatus.Begin));
    }

    @Test
    public void testIsTwoPhaseHeuristic() {
        Assertions.assertTrue(GlobalStatus.isTwoPhaseHeuristic(GlobalStatus.Finished));
        Assertions.assertFalse(GlobalStatus.isTwoPhaseHeuristic(GlobalStatus.Begin));
    }
}
