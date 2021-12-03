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

package io.seata.server;

import java.util.Properties;

import io.seata.common.util.StringUtils;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import static io.seata.common.DefaultValues.SERVICE_OFFSET_SPRING_BOOT;
import static io.seata.core.constants.ConfigurationKeys.ENV_SEATA_PORT_KEY;
import static io.seata.core.constants.ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL;
import static io.seata.core.constants.ConfigurationKeys.SERVER_SERVICE_PORT_CONFIG;

/**
 * @author slievrly
 */
public class ServerApplicationListener implements GenericApplicationListener {

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
        return eventType.getRawClass() != null
                && ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType.getRawClass());
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (!(event instanceof ApplicationEnvironmentPreparedEvent)) {
            return;
        }

        ApplicationEnvironmentPreparedEvent environmentPreparedEvent = (ApplicationEnvironmentPreparedEvent)event;
        ConfigurableEnvironment environment = environmentPreparedEvent.getEnvironment();
        String[] args = environmentPreparedEvent.getArgs();


        // port: -h > -D > env > yml > default

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
        String dPort = environment.getProperty(SERVER_SERVICE_PORT_CAMEL);
        if (StringUtils.isNotBlank(dPort)) {
            setTargetPort(environment, dPort, true);
            return;
        }

        //docker -e SEATA_PORT=8091
        String envPort = environment.getProperty(ENV_SEATA_PORT_KEY);
        if (StringUtils.isNotBlank(envPort)) {
            setTargetPort(environment, envPort, true);
            return;
        }

        //yml properties seata.server.service-port=8091
        String configPort = environment.getProperty(SERVER_SERVICE_PORT_CONFIG);
        if (StringUtils.isNotBlank(configPort)) {
            setTargetPort(environment, configPort, false);
            return;
        }

        // server.port=7091
        String serverPort = environment.getProperty("server.port");
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
