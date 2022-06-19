package io.seata.server.logging.extend;

import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author wlx
 * @date 2022/5/30 11:23 下午
 */
public interface SeataLoggingExtendAppender {

    /**
     * build a Logging Appender whit spring environment
     * and append  it to loggerContext.
     *
     * @param environment environment
     */
    void appendAppender(ConfigurableEnvironment environment);

}
