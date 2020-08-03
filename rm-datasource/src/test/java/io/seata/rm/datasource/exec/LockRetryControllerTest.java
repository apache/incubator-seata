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
package io.seata.rm.datasource.exec;

import io.seata.core.context.GlobalLockConfigHolder;
import io.seata.core.model.GlobalLockConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author selfishlover
 */
class LockRetryControllerTest {

    private GlobalLockConfig config;

    @BeforeEach
    void setUp() {
        config = new GlobalLockConfig();
        config.setLockRetryInternal(10);
        config.setLockRetryTimes(3);
        GlobalLockConfigHolder.setAndReturnPrevious(config);
    }

    @Test
    void testRetryNotExceeded() {
        LockRetryController controller = new LockRetryController();
        assertDoesNotThrow(() -> {
            for (int times = 0; times < config.getLockRetryTimes(); times++) {
                controller.sleep(new RuntimeException("test"));
            }
        }, "should not throw when retry not exceeded");
    }

    @Test
    void testRetryExceeded() {
        LockRetryController controller = new LockRetryController();
        assertThrows(LockWaitTimeoutException.class, () -> {
            for (int times = 0; times <= config.getLockRetryTimes(); times++) {
                controller.sleep(new RuntimeException("test"));
            }
        }, "should throw when retry exceeded");
    }
}