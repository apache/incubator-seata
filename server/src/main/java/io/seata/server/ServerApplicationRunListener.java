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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import static io.seata.common.DefaultValues.SERVICE_OFFSET_SPRING_BOOT;
import static io.seata.core.constants.ConfigurationKeys.ENV_SEATA_PORT_KEY;
import static io.seata.core.constants.ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL;
import static io.seata.core.constants.ConfigurationKeys.SERVER_SERVICE_PORT_CONFIG;

/**
 * @author slievrly
 */
public class ServerApplicationRunListener implements SpringApplicationRunListener, Ordered {

    private final SpringApplication application;
    private final String[] args;
    private String targetPort;

    public ServerApplicationRunListener(SpringApplication sa, String[] args) {
        this.application = sa;
        this.args = args;
    }

    @Override
    public void environmentPrepared(ConfigurableEnvironment environment) {
        // port: -h > -D > env > yml > default

        //-p 8091
        if (args != null && args.length >= 2) {
            for (int i = 0; i < args.length; ++i) {
                if ("-p".equalsIgnoreCase(args[i]) && i < args.length - 1) {
                    setTargetPort(args[i + 1]);
                    return;
                }
            }
        }

        // -Dserver.servicePort=8091
        String dPort = environment.getProperty(SERVER_SERVICE_PORT_CAMEL);
        if (StringUtils.isNotBlank(dPort)) {
            setTargetPort(dPort);
            return;
        }

        //docker -e SEATA_PORT=8091
        String envPort = environment.getProperty(ENV_SEATA_PORT_KEY);
        if (StringUtils.isNotBlank(envPort)) {
            setTargetPort(envPort);
            return;
        }

        //yml properties seata.server.service-port=8091
        String configPort = environment.getProperty(SERVER_SERVICE_PORT_CONFIG);
        if (StringUtils.isNotBlank(configPort)) {
            setTargetPort(configPort);
            return;
        }

        // server.port=7091
        String serverPort = environment.getProperty("server.port");
        if (StringUtils.isBlank(serverPort)) {
            serverPort = "8080";
        }
        targetPort = String.valueOf(Integer.parseInt(serverPort) + SERVICE_OFFSET_SPRING_BOOT);
        setTargetPort(targetPort);
    }

    private void setTargetPort(String port) {
        this.targetPort = port;
        // get rpc port first, use to logback-spring.xml, @see the class named `SystemPropertyLoggerContextListener`
        System.setProperty(SERVER_SERVICE_PORT_CAMEL, port);

    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
        Properties pro = new Properties();
        pro.setProperty(SERVER_SERVICE_PORT_CONFIG, targetPort);
        context.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("serverProperties", pro));
    }

    /**
     * lower than EventPublishingRunListener
     *
     * @return
     */
    @Override
    public int getOrder() {
        return 1;
    }

}
