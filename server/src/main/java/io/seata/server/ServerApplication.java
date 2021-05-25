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

import io.seata.core.constants.ConfigurationKeys;
import io.seata.server.env.PortHelper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author spilledyear@outlook.com
 */
@SpringBootApplication
public class ServerApplication {
    public static void main(String[] args) {
        // get rpc port first, use to logback-spring.xml, @see the class named `SystemPropertyLoggerContextListener`
        int port = PortHelper.getPort(args);
        System.setProperty(ConfigurationKeys.SERVER_RPC_PORT, Integer.toString(port));

        // run the spring-boot application
        SpringApplication.run(ServerApplication.class, args);
    }
}
