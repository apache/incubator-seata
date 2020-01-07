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
package io.seata.integration.test.api.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The type Properties util.
 */
public class PropertiesUtil {

    /**
     * Gets propertie value.
     *
     * @param path the path
     * @param key  the key
     * @return the propertie value
     */
    public static String getPropertieValue(String path, String key) {
        return getPropertieValue(path, key, null);

    }

    /**
     * Gets propertie value.
     *
     * @param path         the path
     * @param key          the key
     * @param defaultValue the default value
     * @return the propertie value
     */
    public static String getPropertieValue(String path, String key, String defaultValue) {
        Properties properties = new Properties();
        InputStream in = PropertiesUtil.class.getClassLoader().getResourceAsStream(path);
        try {
            properties.load(in);
        } catch (IOException ignore) {
        }
        String value = properties.getProperty(key);
        return value == null ? defaultValue : value;
    }
}

