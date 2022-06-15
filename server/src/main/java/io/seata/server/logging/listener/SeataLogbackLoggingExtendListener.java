package io.seata.server.logging.listener;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.server.logging.extend.SeataLoggingExtendAppender;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.List;
import java.util.Objects;


/**
 * the type SeataLogbackLoggingExtendListener
 *
 * @author wlx
 * @date 2022/5/27 11:18 下午
 */
public class SeataLogbackLoggingExtendListener implements GenericApplicationListener {

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        if (Objects.isNull(sourceType)) {
            return false;
        } else {
            return SpringApplication.class.isAssignableFrom(sourceType)
                    || ApplicationContext.class.isAssignableFrom(sourceType);
        }
    }

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
        Class<?> typeRawClass = eventType.getRawClass();
        if (Objects.isNull(typeRawClass)) {
            return false;
        } else {
            return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(typeRawClass);
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        List<SeataLoggingExtendAppender> appenderList = EnhancedServiceLoader.loadAll(SeataLoggingExtendAppender.class);
        ConfigurableEnvironment environment = ((ApplicationEnvironmentPreparedEvent) event).getEnvironment();
        appenderList.forEach(
                appender -> appender.appendAppender(environment)
        );
    }

    @Override
    public int getOrder() {
        // exec after LoggingApplicationListener
        return LoggingApplicationListener.DEFAULT_ORDER + 1;
    }

}
