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
 * Compared with {@link LockRetryControllerTest} and {@link LockRetryControllerTestB},
 * test the compatibility of {@link GlobalLockConfig#getLockRetryInterval()}
 * and {@link GlobalLockConfig#getLockRetryInternal()},the former takes precedence over the latter
 * because {@link GlobalLockConfig#getLockRetryInternal()} will be deleted in v1.6
 *
 * @author linkedme@qq.com
 */
public class LockRetryControllerTestC {

    /**
     * use default
     */
    public static class UseDefaultTest {

        @BeforeEach
        public void setUp() {
            // nothing to do
        }

        @Test
        public void getLockRetryInterval() {
            int defaultRetryInternal = DefaultValues.DEFAULT_CLIENT_LOCK_RETRY_INTERVAL;
            int defaultRetryTimes = DefaultValues.DEFAULT_CLIENT_LOCK_RETRY_TIMES;
            LockRetryController controller = new LockRetryController();

            assertEquals(defaultRetryInternal, controller.getLockRetryInterval(), "test failed");
            assertEquals(defaultRetryTimes, controller.getLockRetryTimes(), "test failed");
        }

        @AfterEach
        public void tearDown() {
            // nothing to do
        }
    }

    /**
     * use only {@link GlobalLockConfig#setLockRetryInternal}
     */
    public static class UseOnlySetLockRetryInternalTest {

        @BeforeEach
        public void setUp() {
            GlobalLockConfig config = new GlobalLockConfig();
            config.setLockRetryInternal(10);
            config.setLockRetryTimes(3);
            GlobalLockConfigHolder.setAndReturnPrevious(config);
        }

        @Test
        public void setLockRetryInterval() {
            LockRetryController controller = new LockRetryController();

            assertEquals(10, controller.getLockRetryInterval(), "test failed");
            assertEquals(3, controller.getLockRetryTimes(), "test failed");
        }

        @AfterEach
        public void tearDown() {
            GlobalLockConfigHolder.remove();
        }
    }

    /**
     * use only {@link GlobalLockConfig#setLockRetryInterval}
     */
    public static class UseOnlySetLockRetryIntervalTest {
        @BeforeEach
        public void setUp() {
            GlobalLockConfig config = new GlobalLockConfig();
            config.setLockRetryInterval(10);
            config.setLockRetryTimes(3);
            GlobalLockConfigHolder.setAndReturnPrevious(config);
        }

        @Test
        public void setLockRetryInterval() {
            LockRetryController controller = new LockRetryController();

            assertEquals(10, controller.getLockRetryInterval(), "test failed");
            assertEquals(3, controller.getLockRetryTimes(), "test failed");
        }

        @AfterEach
        public void tearDown() {
            GlobalLockConfigHolder.remove();
        }
    }

    /**
     * use both {@link GlobalLockConfig#setLockRetryInternal} and {@link GlobalLockConfig#setLockRetryInterval}
     */
    public static class UseBothSetLockRetryInternalAndSetLockRetryIntervalTest {
        @BeforeEach
        public void setUp() {
            GlobalLockConfig config = new GlobalLockConfig();
            config.setLockRetryInternal(10);
            config.setLockRetryInterval(20);
            config.setLockRetryTimes(3);
            GlobalLockConfigHolder.setAndReturnPrevious(config);
        }

        @Test
        public void setLockRetryInterval() {
            LockRetryController controller = new LockRetryController();

            assertEquals(20, controller.getLockRetryInterval(), "test failed");
            assertEquals(3, controller.getLockRetryTimes(), "test failed");
        }

        @AfterEach
        public void tearDown() {
            GlobalLockConfigHolder.remove();
        }
    }

}
