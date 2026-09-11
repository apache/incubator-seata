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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * @author funkye
 */
@SpringBootTest
public class LoaderConfTest {

    static Environment environment;

    /**
     * Init session manager.
     *
     * @throws Exception the exception
     */
    @BeforeAll
    public static void initSessionManager(ApplicationContext context) throws Exception {
        environment = context.getEnvironment();
    }

    @Test
    public void checkConf() {
        String nacosServerAddr = environment.resolveRequiredPlaceholders("${seata.config.nacos.serverAddr:localhost}");
        Assertions.assertEquals("127.0.0.1:8848", nacosServerAddr);
        String nacosNamespace = environment.resolveRequiredPlaceholders("${seata.config.nacos.namespace:seata-group}");
        Assertions.assertEquals("seata-test", nacosNamespace);
        String undologSaveDays = environment.resolveRequiredPlaceholders("${seata.server.undo.log-save-days:7}");
        Assertions.assertEquals("2", undologSaveDays);
    }

}
