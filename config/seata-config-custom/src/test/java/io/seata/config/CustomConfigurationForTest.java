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
import java.util.List;
import java.util.Properties;

public class CustomConfigurationForTest extends AbstractConfiguration<ConfigChangeListener> {
    private Properties properties;

    public CustomConfigurationForTest(String name) {
        try (InputStream input = CustomConfigurationForTest.class.getClassLoader().getResourceAsStream(name)) {
            properties = new Properties();
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getTypeName() {
        return "forTest";
    }

    @Override
    public String getConfig(String dataId, String defaultValue, long timeoutMills) {
        return properties.getProperty(dataId, defaultValue);
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addConfigListener(String dataId, ConfigChangeListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeConfigListener(String dataId, ConfigChangeListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConfigChangeListener> getConfigListeners(String dataId) {
        throw new UnsupportedOperationException();
    }
}
