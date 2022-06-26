package io.seata.server.logging.listener;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import io.seata.server.logging.logback.LogbackExtendConfigurator;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * The type of LoggingLoggerContextListener,to add seata logging extend appender when
 * loggerContext reset.
 * <p>
 * This listener will be appended to loggerContext if necessary by
 * {@link LogbackExtendConfigurator#registerLoggingExtendLoggerContextListenerIfNecessary()}
 *
 * @author wlx
 * @date 2022/6/26 12:46 下午
 * @see ch.qos.logback.classic.spi.LoggerContextListener
 * @see LogbackExtendConfigurator
 */

public class LoggingExtendLoggerContextListener extends ContextAwareBase implements LoggerContextListener, LifeCycle {

    private final ConfigurableEnvironment environment;

    private boolean started = false;

    public LoggingExtendLoggerContextListener(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public boolean isResetResistant() {
        return true;
    }

    @Override
    public void onStart(LoggerContext context) {
        // do nothing,the appender will be appended by LoggingExtendApplicationListener
        // on ApplicationEnvironmentPreparedEvent
    }

    @Override
    public void onReset(LoggerContext context) {
        LogbackExtendConfigurator.get(environment).doLoggingExtendConfiguration();
    }

    @Override
    public void onStop(LoggerContext context) {
        LogbackExtendConfigurator.get(environment).stop();
        stop();
    }

    @Override
    public void onLevelChange(Logger logger, Level level) {
        LogbackExtendConfigurator.get(environment).doLoggingExtendConfiguration();
    }

    @Override
    public void start() {
        if (started) {
            return;
        }
        started = true;
    }

    @Override
    public void stop() {
        started = false;
    }

    @Override
    public boolean isStarted() {
        return started;
    }
}
