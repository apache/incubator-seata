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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.apache.seata.common.Constants.APPLICATION_TYPE_KEY;
import static org.apache.seata.common.Constants.APPLICATION_TYPE_SERVER;

/**
 */
@SpringBootApplication(scanBasePackages = {"org.apache.seata"})
public class ServerApplication {
    public static void main(String[] args) throws IOException {
        System.setProperty(APPLICATION_TYPE_KEY, APPLICATION_TYPE_SERVER);
        // run the spring-boot application
        SpringApplication.run(ServerApplication.class, args);
    }
}
