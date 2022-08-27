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
package io.seata.server.storage.mq.kafka;

import io.seata.common.ConfigurationKeys;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.exception.TransactionException;
import io.seata.server.storage.mq.MqManager;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

//TODO modify name abstract
public class KafkaManager implements MqManager {
    /**
     * The constant LOGGER.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(KafkaManager.class);

    private final KafkaProducer<byte[], byte[]> sessionProducer;

    private static KafkaManager instance;

    private static final Configuration CONFIGURATION = ConfigurationFactory.getInstance();

    private static String kafkaServers;

    private KafkaManager() {
        Properties properties = new Properties();
        kafkaServers = CONFIGURATION.getConfig(ConfigurationKeys.STORE_KAFKA_SERVERS);
        if (StringUtils.isBlank(kafkaServers)) {
            kafkaServers = "localhost:9092";
        }
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, BytesSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, BytesSerializer.class);
        sessionProducer = new KafkaProducer<>(properties);
    }

    public static KafkaManager getInstance() {
        if (instance == null) {
            instance = new KafkaManager();
        }
        return instance;
    }

    public void publish(String topic, byte[] sessionBytes) {
        Future<RecordMetadata> future = sessionProducer.send(new ProducerRecord<>(topic, sessionBytes));
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
