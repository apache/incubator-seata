/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server.spring.listener;

import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;

/**
 * Application listener to clear the SERVER_SERVICE_PORT_CAMEL system property
 * during Spring context environment preparation, refresh, and close events.
 * It runs with higher priority than ServerApplicationListener.
 */
public class ClearServerServicePortInitializer implements GenericApplicationListener, Ordered {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
        Class<?> rawClass = eventType.getRawClass();
        if (rawClass == null) {
            return false;
        }
        // Listen to environment preparation, context refresh, and context close events
        return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(rawClass)
                || ContextClosedEvent.class.isAssignableFrom(rawClass);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        ConfigurationCache.clear();
        // Clear the property for any of the supported events
        System.clearProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
        log.info("Cleared system property: " + ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
    }

    @Override
    public int getOrder() {
        // ServerApplicationListener order is LoggingApplicationListener.DEFAULT_ORDER - 1.
        // This listener needs to run before ServerApplicationListener for ApplicationEnvironmentPreparedEvent.
        return LoggingApplicationListener.DEFAULT_ORDER - 2;
    }
}
