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
package org.apache.seata.spring.boot.autoconfigure.listener;

import org.apache.seata.common.holder.ObjectHolder;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;


public class SeataApplicationListener implements GenericApplicationListener {

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
        return eventType.getRawClass() != null
                && (ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType.getRawClass()) ||
                ApplicationReadyEvent.class.isAssignableFrom(eventType.getRawClass()));
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (!(event instanceof ApplicationEnvironmentPreparedEvent)) {
            return;
        }
        ApplicationEnvironmentPreparedEvent environmentPreparedEvent = (ApplicationEnvironmentPreparedEvent)event;
        ConfigurableEnvironment environment = environmentPreparedEvent.getEnvironment();
        ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, environment);
    }

    /**
     * higher than LoggingApplicationListener
     *
     * @return the order
     */
    @Override
    public int getOrder() {
        return LoggingApplicationListener.DEFAULT_ORDER - 1;
    }
}