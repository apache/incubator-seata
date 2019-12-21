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
package io.seata.server.lock;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;

/**
 * The type Lock manager factory.
 *
 * @author sharajava
 */
public class LockerFactory {

    /**
     * The constant lockManager.
     */
    private static volatile LockManager lockManager;

    /**
     * Get lock manager.
     *
     * @return the lock manager
     */
    public static LockManager getLockManager() {
        if (lockManager == null) {
            synchronized (LockerFactory.class) {
                if (lockManager == null) {
                    lockManager = EnhancedServiceLoader.load(LockManager.class,
                            ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.STORE_MODE));
                }
            }
        }
        return lockManager;
    }
}
