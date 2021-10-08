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
package io.seata.server.storage.hbase;

import io.seata.config.ConfigurationFactory;
import io.seata.config.Configuration;
import io.seata.core.constants.ConfigurationKeys;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.io.IOException;

/**
 * @author haishin
 */
public class HBaseSingleConnectionFactory {
    /**
     * The constant LOGGER.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(HBaseSingleConnectionFactory.class);

    private volatile static Connection connection = null;

    private static final String QUORUM = "hadoop1";

    private static final int PORT = 2181;

    private static final int POOL_SIZE = 1;

    private static final String POOL_TYPE = "Reusable";

    private static final String RETRIES = "3";

    private static final Configuration CONFIGURATION = ConfigurationFactory.getInstance();

    public static Connection getInstance() {
        if (connection == null) {
            synchronized (Connection.class) {
                if (connection == null) {
                    try {
                        org.apache.hadoop.conf.Configuration configuration = HBaseConfiguration.create();
                        configuration.set("hbase.zookeeper.quorum", CONFIGURATION.getConfig(ConfigurationKeys.STORE_HBASE_ZOOKEEPER_QUORUM, QUORUM));
                        configuration.set("hbase.zookeeper.property.clientPort", CONFIGURATION.getConfig(ConfigurationKeys.STORE_HBASE_PROPERTY_CLIENT_PORT, PORT));
                        configuration.set("hbase.client.ipc.pool.type", CONFIGURATION.getConfig(ConfigurationKeys.STORE_HBASE_POOL_TYPE, POOL_TYPE));
                        configuration.set("hbase.client.ipc.pool.size", CONFIGURATION.getConfig(ConfigurationKeys.STORE_HBASE_POOL_SIZE, POOL_SIZE));
                        configuration.set("hbase.client.retries.number", RETRIES);
                        connection = ConnectionFactory.createConnection(configuration);
                        LOGGER.info("the connection of HBase is created successfully.");
                    } catch (IOException e) {
                        LOGGER.error("the connection of HBase is created failed.", e.getMessage());
                    }
                }
            }
        }
        return connection;
    }

    @PreDestroy
    private void closeConnection() {
        try {
            connection.close();
        } catch (IOException e) {
            LOGGER.error("failure to close the connection of HBase!");
        }

    }
}


