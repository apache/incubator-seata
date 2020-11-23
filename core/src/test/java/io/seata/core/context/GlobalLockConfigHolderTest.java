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
package io.seata.core.context;

import io.seata.core.model.GlobalLockConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalLockConfigHolderTest {

    @BeforeEach
    void setUp() {
        assertNull(GlobalLockConfigHolder.getCurrentGlobalLockConfig(), "should be null at first");
    }

    @Test
    void setAndReturnPrevious() {
        GlobalLockConfig config1 = new GlobalLockConfig();
        assertNull(GlobalLockConfigHolder.setAndReturnPrevious(config1), "should return null");
        assertSame(config1, GlobalLockConfigHolder.getCurrentGlobalLockConfig(), "holder fail to store config");

        GlobalLockConfig config2 = new GlobalLockConfig();
        assertSame(config1, GlobalLockConfigHolder.setAndReturnPrevious(config2), "fail to get previous config");
        assertSame(config2, GlobalLockConfigHolder.getCurrentGlobalLockConfig(), "holder fail to store latest config");
    }

    @AfterEach
    void tearDown() {
        assertDoesNotThrow(GlobalLockConfigHolder::remove, "clear method should not throw anything");
    }
}