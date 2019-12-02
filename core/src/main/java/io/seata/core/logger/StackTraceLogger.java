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
