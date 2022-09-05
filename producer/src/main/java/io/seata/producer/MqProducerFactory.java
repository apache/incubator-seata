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
package io.seata.producer;

import io.seata.common.ConfigurationKeys;
import io.seata.config.ConfigurationFactory;

import java.util.ServiceLoader;

public class MqProducerFactory {

    private static volatile MqProducer instance = null;

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static MqProducer getInstance() {
        if (instance == null) {
            synchronized (MqProducer.class) {
                if (instance == null) {
                    instance = loadProducer();
                }
            }
        }
        return instance;
    }

    private static MqProducer loadProducer() {
        String defaultProducerName = "kafka";
        String producerName = ConfigurationFactory.getInstance()
                .getConfig(ConfigurationKeys.STORE_MQ_MODE, defaultProducerName).toLowerCase();
        ServiceLoader<MqProducer> mqProducers = ServiceLoader.load(MqProducer.class);
        for (MqProducer mqProducer : mqProducers) {
            String className = mqProducer.getClass().getSimpleName();
            if (className.substring(0, className.indexOf("Producer")).toLowerCase().equals(producerName)) {
                return mqProducer;
            }
        }
        throw new IllegalArgumentException("Load MqProducer fail.");
    }
}
