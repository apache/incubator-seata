package io.seata.server.logging.listener;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import io.seata.server.logging.logback.LogbackCompositeLoggingExtendProvider;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * The type of LoggingLoggerContextListener,to add seata logging extend appender when
 * loggerContext start or reset.
 * <p>
 * This listener will auto append to loggerContext if necessary by
 * {@link LogbackCompositeLoggingExtendProvider#registerLoggerContextListenerIfNecessary(ConfigurableEnvironment)}
 * <p>
 * You can also add it to logback.xml like this
 * <pre>
 *      &lt;contextListener class="io.seata.server.logging.listener.LoggingExtendApplicationListener"/&gt;
 * </pre>
 *
 * @author wlx
 * @date 2022/6/24 8:35 下午
 * @see ch.qos.logback.classic.spi.LoggerContextListener
 * @see LogbackCompositeLoggingExtendProvider
 */
public class LoggingLoggerContextListener extends ContextAwareBase implements LoggerContextListener, LifeCycle {

    private boolean started = false;

    private final ConfigurableEnvironment environment;

    public LoggingLoggerContextListener(ConfigurableEnvironment environment) {
        this.environment = environment;
        this.started = true;
    }

    /**
     * LoggingLoggerContextListener should not be removed subsequent to a LoggerContext reset.
     *
     * @return true
     */
    @Override
    public boolean isResetResistant() {
        return true;
    }

    @Override
    public void onStart(LoggerContext context) {
        LogbackCompositeLoggingExtendProvider provider = LogbackCompositeLoggingExtendProvider.get(environment);
        provider.appendToContext();
    }

    /**
     * when reset append logging extend appender to LoggerContext
     *
     * @param context context
     */
    @Override
    public void onReset(LoggerContext context) {
        if (context.isStarted()) {
            LogbackCompositeLoggingExtendProvider provider = LogbackCompositeLoggingExtendProvider.get(environment);
            provider.appendToContext();
        }
    }

    @Override
    public void onStop(LoggerContext context) {

    }

    @Override
    public void onLevelChange(Logger logger, Level level) {

    }

    @Override
    public void start() {
        if (started) {
            return;
        }
        this.started = true;
    }

    @Override
    public void stop() {
        if (started) {
            this.started = false;
        }
    }

    @Override
    public boolean isStarted() {
        return started;
    }
}
