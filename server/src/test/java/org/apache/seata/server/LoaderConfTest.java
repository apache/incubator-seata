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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 *
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
        Assertions.assertEquals(nacosServerAddr, "127.0.0.1:8848");
        String nacosNamespace = environment.resolveRequiredPlaceholders("${seata.config.nacos.namespace:seata-group}");
        Assertions.assertEquals(nacosNamespace, "seata-test");
        String undologSaveDays = environment.resolveRequiredPlaceholders("${seata.server.undo.log-save-days:7}");
        Assertions.assertEquals(undologSaveDays, "2");
    }
    

}
