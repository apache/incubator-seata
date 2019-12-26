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
package io.seata.server.coordinator;

/**
 * The type Core factory.
 *
 * @author sharajava
 */
public class CoreFactory {

    private static class SingletonHolder {
        private static Core INSTANCE = new DefaultCore();
    }

    /**
     * Get core.
     *
     * @return the core
     */
    public static final Core get() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Just for test mocking
     *
     * @param core the core
     */
    public static void set(Core core) {
        SingletonHolder.INSTANCE = core;
    }
}
