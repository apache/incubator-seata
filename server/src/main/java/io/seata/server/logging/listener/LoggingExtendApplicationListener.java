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
package io.seata.server.logging.listener;

import io.seata.server.logging.logback.LogbackExtendConfigurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Objects;


/**
 * the type LoggingExtendApplicationListener, listen ApplicationEnvironmentPreparedEvent
 * to load logging appender
 *
 * @author wlx
 * @see org.springframework.context.event.GenericApplicationListener
 * @see ApplicationEnvironmentPreparedEvent
 */
public class LoggingExtendApplicationListener implements GenericApplicationListener {


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
        ConfigurableEnvironment environment = ((ApplicationEnvironmentPreparedEvent) event).getEnvironment();
        LogbackExtendConfigurator configurator = LogbackExtendConfigurator.get(environment);
        configurator.doLoggingExtendConfiguration();
    }

    @Override
    public int getOrder() {
        // exec after LoggingApplicationListener
        return LoggingApplicationListener.DEFAULT_ORDER + 1;
    }

}
