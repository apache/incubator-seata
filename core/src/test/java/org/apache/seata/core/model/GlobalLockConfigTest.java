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

import org.apache.seata.common.LockStrategyMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The GlobalLockConfig Test
 */
public class GlobalLockConfigTest {

    @Test
    public void testGetLockTimeout() {
        GlobalLockConfig config = new GlobalLockConfig();
        config.setLockRetryTimes(5000);
        assertEquals(5000, config.getLockRetryTimes());
    }

    @Test
    public void testGetLockRetryInterval() {
        GlobalLockConfig config = new GlobalLockConfig();
        config.setLockRetryInterval(1000);
        assertEquals(1000, config.getLockRetryInterval());
    }

    @Test
    public void testIsLockEnabled() {
        GlobalLockConfig config = new GlobalLockConfig();
        config.setLockStrategyMode(LockStrategyMode.OPTIMISTIC);
        assertEquals(LockStrategyMode.OPTIMISTIC, config.getLockStrategyMode());
    }
}
