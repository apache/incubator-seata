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
package io.seata.server.storage.mq;

import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;

import java.util.ServiceLoader;

public class MqManagerFactory {

    private static volatile MqManager instance = null;

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static MqManager getInstance() {
        if (instance == null) {
            synchronized (MqManager.class) {
                if (instance == null) {
                    instance = buildMqManager();
                }
            }
        }
        return instance;
    }


    private static MqManager buildMqManager() {
        String defaultManagerName = "kafka";
        String managerName = ConfigurationFactory.getInstance().getConfig("store.mq.mode",defaultManagerName).toLowerCase();
        ServiceLoader<MqManager> mqManagers = ServiceLoader.load(MqManager.class);
        for (MqManager mqManager : mqManagers) {
            String className = mqManager.getClass().getSimpleName();
            if (className.substring(0, className.indexOf("Manager")).toLowerCase().equals(managerName)) {
                return mqManager;
            }
        }
        throw new IllegalArgumentException("don't load mqManager");
    }}
