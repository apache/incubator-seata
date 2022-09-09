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
package io.seata.consumer;

import io.seata.common.ConfigurationKeys;
import io.seata.common.DefaultValues;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.config.ConfigurationFactory;

import java.util.ServiceLoader;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MqConsumerManager {
    private final ExecutorService executorService =
            new ThreadPoolExecutor(1, 1, 3, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1), new NamedThreadFactory("MqConsumer", 1));

    public MqConsumerManager() {
        consume();
    }

    public void consume() {
        MqConsumer mqConsumer = loadConsumer();
        executorService.submit(mqConsumer);
    }

    private MqConsumer loadConsumer() {
        String consumerName = ConfigurationFactory.getInstance()
                .getConfig(ConfigurationKeys.STORE_MQ_MODE, DefaultValues.DEFAULT_STORE_MQ_MODE).toLowerCase();
        ServiceLoader<MqConsumer> mqConsumers = ServiceLoader.load(MqConsumer.class);
        for (MqConsumer mqConsumer : mqConsumers) {
            String className = mqConsumer.getClass().getSimpleName();
            if (className.substring(0, className.indexOf("Consumer")).toLowerCase().equals(consumerName)) {
                return mqConsumer;
            }
        }
        throw new RuntimeException(String.format("Load consumer[%s] fail.", consumerName));
    }
}
