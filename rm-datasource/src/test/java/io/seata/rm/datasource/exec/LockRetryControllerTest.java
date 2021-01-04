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

import io.seata.common.DefaultValues;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.context.GlobalLockConfigHolder;
import io.seata.core.model.GlobalLockConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author selfishlover
 */
public class LockRetryControllerTest {

    private GlobalLockConfig config;

    private final int defaultRetryInternal = DefaultValues.DEFAULT_CLIENT_LOCK_RETRY_INTERVAL;
    private final int defaultRetryTimes = DefaultValues.DEFAULT_CLIENT_LOCK_RETRY_TIMES;

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
        }, "should not throw anything when retry not exceeded");
    }

    @Test
    void testRetryExceeded() {
        LockRetryController controller = new LockRetryController();
        assertThrows(LockWaitTimeoutException.class, () -> {
            for (int times = 0; times <= config.getLockRetryTimes(); times++) {
                controller.sleep(new RuntimeException("test"));
            }
        }, "should throw LockWaitTimeoutException when retry exceeded");
    }

    @Test
    void testNoCustomizedConfig() {
        GlobalLockConfigHolder.remove();
        LockRetryController controller = new LockRetryController();
        String message = "should use global config when there is no customized config";
        assertEquals(defaultRetryInternal, controller.getLockRetryInternal(), message);
        assertEquals(defaultRetryTimes, controller.getLockRetryTimes(), message);
    }

    @Test
    void testLockConfigListener() {
        LockRetryController.GlobalConfig config = new LockRetryController.GlobalConfig();
        ConfigurationChangeEvent event = new ConfigurationChangeEvent();

        event.setDataId(ConfigurationKeys.CLIENT_LOCK_RETRY_INTERVAL);
        int retryInterval = 100;
        event.setNewValue(retryInterval + "");
        config.onChangeEvent(event);
        String message1 = "lock config listener fail to update latest value of CLIENT_LOCK_RETRY_INTERVAL";
        assertEquals(retryInterval, config.getGlobalLockRetryInternal(), message1);

        event.setDataId(ConfigurationKeys.CLIENT_LOCK_RETRY_TIMES);
        int retryTimes = 5;
        event.setNewValue(retryTimes + "");
        config.onChangeEvent(event);
        String message2 = "lock config listener fail to update latest value of CLIENT_LOCK_RETRY_TIMES";
        assertEquals(retryTimes, config.getGlobalLockRetryTimes(), message2);

        event.setDataId(ConfigurationKeys.CLIENT_LOCK_RETRY_INTERVAL);
        event.setNewValue("not a number");
        config.onChangeEvent(event);
        String message3 = "should fallback to default value when receive an illegal config value of CLIENT_LOCK_RETRY_INTERVAL";
        assertEquals(defaultRetryInternal, config.getGlobalLockRetryInternal(), message3);

        event.setDataId(ConfigurationKeys.CLIENT_LOCK_RETRY_TIMES);
        event.setNewValue("not a number");
        config.onChangeEvent(event);
        String message4 = "should fallback to default value when receive an illegal config value of CLIENT_LOCK_RETRY_TIMES";
        assertEquals(defaultRetryTimes, config.getGlobalLockRetryTimes(), message4);
    }

    @AfterEach
    void tearDown() {
        GlobalLockConfigHolder.remove();
    }
}