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
package org.apache.seata.compatibility.raft;

import org.apache.seata.config.ConfigurationFactory;

import java.lang.reflect.Method;

final class LegacyConfigurationHelper {

    private LegacyConfigurationHelper() {}

    static void put(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    static void clear(String key) {
        System.clearProperty(key);
    }

    static void reload() {
        try {
            Method method = ConfigurationFactory.class.getDeclaredMethod("reload");
            method.setAccessible(true);
            method.invoke(null);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to reload seata configuration", e);
        }
    }
}
