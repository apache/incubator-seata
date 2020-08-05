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

import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;

import static io.seata.common.DefaultValues.DEFAULT_CLIENT_LOCK_RETRY_INTERVAL;
import static io.seata.common.DefaultValues.DEFAULT_CLIENT_LOCK_RETRY_TIMES;

/**
 * The type Lock retry controller.
 *
 * @author sharajava
 */
public class LockRetryController {
    private static int LOCK_RETRY_INTERNAL =
        ConfigurationFactory.getInstance().getInt(ConfigurationKeys.CLIENT_LOCK_RETRY_INTERVAL, DEFAULT_CLIENT_LOCK_RETRY_INTERVAL);
    private static int LOCK_RETRY_TIMES =
        ConfigurationFactory.getInstance().getInt(ConfigurationKeys.CLIENT_LOCK_RETRY_TIMES, DEFAULT_CLIENT_LOCK_RETRY_TIMES);

    private int lockRetryInternal = LOCK_RETRY_INTERNAL;
    private int lockRetryTimes = LOCK_RETRY_TIMES;

    /**
     * Instantiates a new Lock retry controller.
     */
    public LockRetryController() {
    }

    /**
     * Sleep.
     *
     * @param e the e
     * @throws LockWaitTimeoutException the lock wait timeout exception
     */
    public void sleep(Exception e) throws LockWaitTimeoutException {
        if (--lockRetryTimes < 0) {
            throw new LockWaitTimeoutException("Global lock wait timeout", e);
        }

        try {
            Thread.sleep(lockRetryInternal);
        } catch (InterruptedException ignore) {
        }
    }
}