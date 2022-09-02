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
package io.seata.consumer.kafka;

import io.seata.common.ConfigurationKeys;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.consumer.MqConsumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import java.util.Properties;


public class KafkaConsumer implements MqConsumer {

    private static final Configuration CONFIGURATION = ConfigurationFactory.getInstance();

    public KafkaConsumer() {
        Properties properties = new Properties();
        String defaultKafkaServer = "localhost:9092";
        String kafkaServers = CONFIGURATION.getConfig(ConfigurationKeys.STORE_KAFKA_SERVERS, defaultKafkaServer);
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

    }

    @Override
    public void consume() {
        //TODO 构造消费者，进行消费 使用console的配置文件，类库没法引用
        //TODO 调用influxdb插入函数

    }
}
