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

import java.util.Properties;

import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.spring.boot.autoconfigure.SeataCoreEnvironmentPostProcessor;
import org.apache.seata.spring.boot.autoconfigure.SeataServerEnvironmentPostProcessor;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.apache.seata.common.DefaultValues.SERVICE_OFFSET_SPRING_BOOT;
import static org.apache.seata.core.constants.ConfigurationKeys.ENV_SEATA_PORT_KEY;
import static org.apache.seata.core.constants.ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL;
import static org.apache.seata.core.constants.ConfigurationKeys.SERVER_SERVICE_PORT_CONFIG;

/**
 */
public class ServerApplicationListener implements GenericApplicationListener {

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
        return eventType.getRawClass() != null
                && (ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType.getRawClass()) ||
                ApplicationReadyEvent.class.isAssignableFrom(eventType.getRawClass()));
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationReadyEvent && Boolean.parseBoolean(System.getProperty("production.deploy.output"))) {
            System.setProperty("ENV_LOG_SYS_BOOT_COMPLETED", "true");
            return;
        }
        if (!(event instanceof ApplicationEnvironmentPreparedEvent)) {
            return;
        }
        ApplicationEnvironmentPreparedEvent environmentPreparedEvent = (ApplicationEnvironmentPreparedEvent)event;
        ConfigurableEnvironment environment = environmentPreparedEvent.getEnvironment();
        ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, environment);
        SeataCoreEnvironmentPostProcessor.init();
        SeataServerEnvironmentPostProcessor.init();

        String[] args = environmentPreparedEvent.getArgs();

        // port: -p > -D > env > yml > default

        //-p 8091
        if (args != null && args.length >= 2) {
            for (int i = 0; i < args.length; ++i) {
                if ("-p".equalsIgnoreCase(args[i]) && i < args.length - 1) {
                    setTargetPort(environment, args[i + 1], true);
                    return;
                }
            }
        }

        // -Dserver.servicePort=8091
        String dPort = environment.getProperty(SERVER_SERVICE_PORT_CAMEL, String.class);
        if (StringUtils.isNotBlank(dPort)) {
            setTargetPort(environment, dPort, true);
            return;
        }

        //docker -e SEATA_PORT=8091
        String envPort = environment.getProperty(ENV_SEATA_PORT_KEY, String.class);
        if (StringUtils.isNotBlank(envPort)) {
            setTargetPort(environment, envPort, true);
            return;
        }

        //yml properties server.service-port=8091
        String configPort = environment.getProperty(SERVER_SERVICE_PORT_CONFIG, String.class);
        if (StringUtils.isNotBlank(configPort)) {
            setTargetPort(environment, configPort, false);
            return;
        }

        // server.port=7091
        String serverPort = environment.getProperty("server.port", String.class);
        if (StringUtils.isBlank(serverPort)) {
            serverPort = "8080";
        }
        String servicePort = String.valueOf(Integer.parseInt(serverPort) + SERVICE_OFFSET_SPRING_BOOT);
        setTargetPort(environment, servicePort, true);
    }

    private void setTargetPort(ConfigurableEnvironment environment, String port, boolean needAddPropertySource) {
        // get rpc port first, use to logback-spring.xml, @see the class named `SystemPropertyLoggerContextListener`
        System.setProperty(SERVER_SERVICE_PORT_CAMEL, port);
        if (needAddPropertySource) {
            // add property source to the first position
            Properties pro = new Properties();
            pro.setProperty(SERVER_SERVICE_PORT_CONFIG, port);
            environment.getPropertySources().addFirst(new PropertiesPropertySource("serverProperties", pro));
        }
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
