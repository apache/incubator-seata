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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.annotation.Nonnull;

import io.seata.config.source.ConfigSource;

import static io.seata.config.source.ConfigSourceOrdered.CONFIG_CENTER_SOURCE_ORDER;

public class CustomConfigSourceForTest implements ConfigSource {
    private Properties properties;
    private String name;

    public CustomConfigSourceForTest(String name) {
        this.name = name;
        try (InputStream input = CustomConfigSourceForTest.class.getClassLoader().getResourceAsStream(name)) {
            properties = new Properties();
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return "forTest: " + name;
    }

    @Override
    public int getOrder() {
        return CONFIG_CENTER_SOURCE_ORDER;
    }

    @Override
    public String getLatestConfig(String dataId, long timeoutMills) {
        return properties.getProperty(dataId);
    }
}
