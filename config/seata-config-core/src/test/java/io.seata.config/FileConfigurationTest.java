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

package io.seata.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author jimin.jm@alibaba-inc.com
 * @date 2019/11/18
 */
class FileConfigurationTest {

    private Configuration fileConfig = ConfigurationFactory.getInstance();

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void addConfigListener() throws InterruptedException {


        //System.out.println(fileConfig.getConfig("service.disableGlobalTransaction"));
        //Thread.sleep(30*1000);
        //fileConfig = ConfigurationFactory.refersh();
        //System.out.println(fileConfig.getConfig("service.disableGlobalTransaction"));
        fileConfig.addConfigListener("service.disableGlobalTransaction", new ConfigurationChangeListener() {
            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {
                System.out.println("old:" + event.getOldValue() + ", new:" + event.getNewValue());
            }
        });
        for(int i=0;i<100;i++) {
            System.setProperty("service.disableGlobalTransaction", String.valueOf(!fileConfig.getBoolean("service" +
                ".disableGlobalTransaction")));
            Thread.sleep(200);
        }
    }

}