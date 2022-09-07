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
import io.seata.consumer.Constants;
import io.seata.consumer.MqConsumer;
import io.seata.consumer.handler.InfluxDBHandler;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;


public class KafkaConsumer extends MqConsumer {
    private static final Configuration CONFIGURATION = ConfigurationFactory.getInstance();

    private final org.apache.kafka.clients.consumer.KafkaConsumer<byte[], byte[]> kafkaConsumer;
    private final InfluxDBHandler influxDBHandler = new InfluxDBHandler();
    private final AtomicBoolean started = new AtomicBoolean(true);

    public KafkaConsumer() {
        Properties properties = new Properties();
        String defaultKafkaServer = "localhost:9092";
        String kafkaServers = CONFIGURATION.getConfig(ConfigurationKeys.STORE_KAFKA_SERVERS, defaultKafkaServer);
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        kafkaConsumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(properties);
        ArrayList<String> topics = new ArrayList<>();
        topics.add(Constants.globalSessionTopic);
        topics.add(Constants.branchSessionTopic);
        topics.add(Constants.undoTopic);
        kafkaConsumer.subscribe(topics);
    }

    @Override
    public void consume() {
        try {
            while (started.get()) {
                ConsumerRecords<byte[], byte[]> records = kafkaConsumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<byte[], byte[]> record : records) {
                    influxDBHandler.handle(record.topic(), record.key(), record.value());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            kafkaConsumer.close();
        }
    }

    public boolean isStarted() {
        return started.get();
    }

    public void start() {
        started.compareAndSet(false, true);
    }

    public void stop() {
        started.compareAndSet(true, false);
    }
}
