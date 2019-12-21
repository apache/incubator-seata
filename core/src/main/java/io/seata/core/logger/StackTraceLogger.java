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
package io.seata.core.logger;

import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import org.slf4j.Logger;

/**
 * @author jsbxyyx
 */
public final class StackTraceLogger {

    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    public static void info(Logger logger, Throwable cause, String format1, Object[] args1, String format2, Object[] args2) {
        if (logger.isInfoEnabled()) {
            int rate = CONFIG.getInt(ConfigurationKeys.TRANSACTION_LOG_EXCEPTION_RATE, 100);
            if (System.currentTimeMillis() % rate == 0) {
                logger.info(format1, args1, cause);
            } else {
                logger.info(format2, args2);
            }
        }
    }

}
