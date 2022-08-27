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
package io.seata.server.storage.mq.session.kafka;

import io.seata.common.ConfigurationKeys;
import io.seata.core.exception.TransactionException;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class KafkaSessionManager {
    private final KafkaProducer<String, GlobalSession> globalSessionProducer;
    private final KafkaProducer<String, BranchSession> branchSessionProducer;

    private static KafkaSessionManager instance;

    private KafkaSessionManager() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        globalSessionProducer = new KafkaProducer<>(properties);
        branchSessionProducer = new KafkaProducer<>(properties);
    }

    public static KafkaSessionManager getInstance() {
        if (instance == null) {
            instance = new KafkaSessionManager();
        }
        return instance;
    }

    public void publish(GlobalSession session) throws TransactionException {
        Future<RecordMetadata> future =
                globalSessionProducer.send(new ProducerRecord<>(ConfigurationKeys.STORE_DB_GLOBAL_TABLE, session));
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new TransactionException(e);
        }
    }

    public void publish(BranchSession branchSession) throws TransactionException {
        Future<RecordMetadata> future =
                branchSessionProducer.send(new ProducerRecord<>(ConfigurationKeys.STORE_DB_BRANCH_TABLE, branchSession));
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new TransactionException(e);
        }
    }
}
