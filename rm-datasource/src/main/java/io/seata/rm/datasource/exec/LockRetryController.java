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

import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.context.GlobalLockConfigHolder;
import io.seata.core.model.GlobalLockConfig;

import static io.seata.common.DefaultValues.DEFAULT_CLIENT_LOCK_RETRY_INTERVAL;
import static io.seata.common.DefaultValues.DEFAULT_CLIENT_LOCK_RETRY_TIMES;

/**
 * The type Lock retry controller.
 *
 * @author sharajava
 */
public class LockRetryController {

    private int lockRetryInternal;

    private int lockRetryTimes;

    /**
     * Instantiates a new Lock retry controller.
     */
    public LockRetryController() {
        this.lockRetryInternal = getLockRetryInternal();
        this.lockRetryTimes = getLockRetryTimes();
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

    private int getLockRetryInternal() {
        // get customized config first
        GlobalLockConfig config = GlobalLockConfigHolder.getCurrentGlobalLockConfig();
        if (config != null) {
            int configInternal = config.getLockRetryInternal();
            if (configInternal > 0) {
                return configInternal;
            }
        }
        // if there is no customized config, use global config instead
        Configuration configuration = ConfigurationFactory.getInstance();
        return configuration.getInt(ConfigurationKeys.CLIENT_LOCK_RETRY_INTERVAL, DEFAULT_CLIENT_LOCK_RETRY_INTERVAL);
    }

    private int getLockRetryTimes() {
        // get customized config first
        GlobalLockConfig config = GlobalLockConfigHolder.getCurrentGlobalLockConfig();
        if (config != null) {
            int configTimes = config.getLockRetryTimes();
            if (configTimes >= 0) {
                return configTimes;
            }
        }
        // if there is no customized config, use global config instead
        Configuration configuration = ConfigurationFactory.getInstance();
        return configuration.getInt(ConfigurationKeys.CLIENT_LOCK_RETRY_TIMES, DEFAULT_CLIENT_LOCK_RETRY_TIMES);
    }
}