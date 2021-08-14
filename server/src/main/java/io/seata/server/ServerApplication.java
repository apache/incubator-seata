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

import java.io.IOException;

import io.seata.common.util.StringUtils;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.server.env.PortHelper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static io.seata.common.DefaultValues.SERVER_PORT;
import static io.seata.common.DefaultValues.SERVICE_OFFSET_SPRING_BOOT;

/**
 * @author spilledyear@outlook.com
 */
@SpringBootApplication
public class ServerApplication {
    public static void main(String[] args) throws IOException {
        // get rpc port first, use to logback-spring.xml, @see the class named `SystemPropertyLoggerContextListener`
        // port: env,-h > -D > default
        int port = PortHelper.getPortFromEnvOrStartup(args);
        if (port != 0) {
            System.setProperty(ConfigurationKeys.SERVER_RPC_PORT, Integer.toString(port));
        }

        if (StringUtils.isBlank(System.getProperty(ConfigurationKeys.SERVER_RPC_PORT))) {
            int serverPort = PortHelper.getPortFromConfigFile();
            int servicePort = serverPort + SERVICE_OFFSET_SPRING_BOOT;
            System.setProperty(SERVER_PORT, String.valueOf(serverPort));
            System.setProperty(ConfigurationKeys.SERVER_RPC_PORT, Integer.toString(servicePort));
        }
        // run the spring-boot application
        SpringApplication.run(ServerApplication.class, args);

    }
}
