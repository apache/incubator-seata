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
package org.apache.seata.server;

import java.io.IOException;

import org.apache.seata.core.protocol.Version;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Server Application
 */
@SpringBootApplication(scanBasePackages = {"org.apache.seata"})
public class ServerApplication {
    public static void main(String[] args) throws IOException {
        // run the spring-boot application
        SpringApplication app = new SpringApplication(ServerApplication.class);
        // set the banner and version information
        app.setBanner((environment, sourceClass, out) -> {
            out.println("====================================================================");
            out.println("                           _             _____            _        \n" +
                    "    /\\                    | |           / ____|          | |       \n" +
                    "   /  \\   _ __   __ _  ___| |__   ___  | (___   ___  __ _| |_ __ _ \n" +
                    "  / /\\ \\ | '_ \\ / _` |/ __| '_ \\ / _ \\  \\___ \\ / _ \\/ _` | __/ _` |\n" +
                    " / ____ \\| |_) | (_| | (__| | | |  __/  ____) |  __/ (_| | || (_| |\n" +
                    "/_/    \\_\\ .__/ \\__,_|\\___|_| |_|\\___| |_____/ \\___|\\__,_|\\__\\__,_|\n" +
                    "         | |                                                       \n" +
                    "         |_|                                                       \n");
            out.println("                        Current Version: " + Version.getCurrent());
            out.println("====================================================================");
        });
        app.run(args);
    }
}
