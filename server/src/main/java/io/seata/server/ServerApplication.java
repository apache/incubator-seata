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

import io.seata.common.aot.NativeUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author spilledyear@outlook.com
 */
@SpringBootApplication(scanBasePackages = {"io.seata"})
public class ServerApplication {

    public static void main(String[] args) throws Throwable {
        try {
            // run the spring-boot application
            SpringApplication.run(ServerApplication.class, args);
        } catch (Throwable t) {
            // This exception is used to end `spring-boot-maven-plugin:process-aot`, so ignore it.
            if ("org.springframework.boot.SpringApplication$AbandonedRunException".equals(t.getClass().getName())) {
                throw t;
            }

            // In the `native-image`, if an exception occurs prematurely during the startup process, the exception log will not be recorded,
            // so here we sleep for 20 seconds to observe the exception information.
            if (NativeUtils.inNativeImage()) {
                t.printStackTrace();
                Thread.sleep(20000);
            }

            throw t;
        }
    }

}
