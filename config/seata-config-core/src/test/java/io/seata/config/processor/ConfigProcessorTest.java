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
package io.seata.config.processor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Properties;


class ConfigProcessorTest {

    @Test
    void processConfig() throws IOException {
        String yamlString = "store:\n" +
                "  mode: db\n" +
                "  db: \n" +
                "    datasource: druid\n" +
                "    dbType: mysql\n" +
                "    driverClassName: com.mysql.jdbc.Driver\n" +
                "    url: jdbc:mysql://127.0.0.1:3306/server_seata\n" +
                "    user: root\n" +
                "    password: 'root'\n";

        final Properties properties = ConfigProcessor.processConfig(yamlString, "yaml");
        Assertions.assertEquals(properties.getProperty("store.mode"), "db");

    }


    @Test
    void resolverConfigDataType() {
        String dataType;

        dataType = ConfigProcessor.resolverConfigDataType("yaml", "a.yaml", "properties");
        Assertions.assertEquals(dataType, "yaml");

        dataType = ConfigProcessor.resolverConfigDataType("", "a.yaml", "properties");
        Assertions.assertEquals(dataType, "yaml");

        dataType = ConfigProcessor.resolverConfigDataType("", "a.txt", "properties");
        Assertions.assertEquals(dataType, "properties");

        dataType = ConfigProcessor.resolverConfigDataType("", "a", "properties");
        Assertions.assertEquals(dataType, "properties");

    }


}