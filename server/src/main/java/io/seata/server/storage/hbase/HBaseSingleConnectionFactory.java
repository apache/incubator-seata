package io.seata.server.storage.hbase;


import io.seata.config.ConfigurationFactory;
import io.seata.config.Configuration;
import io.seata.core.constants.ConfigurationKeys;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author haishin
 */
public class HBaseSingleConnectionFactory {
    /**
     * The constant LOGGER.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(HBaseSingleConnectionFactory.class);

    private static Admin admin = null;

    private volatile static Connection connection = null;

    private static final int PORT = 2181;

    private static final int POOL_SIZE = 1;

    private static final String POOL_TYPE = "Reusable";

    private static final Configuration CONFIGURATION = ConfigurationFactory.getInstance();

    public static Connection getInstance() {
        if (connection == null) {
            synchronized (Connection.class) {
                if (connection == null) {
                    try {
                        org.apache.hadoop.conf.Configuration configuration = HBaseConfiguration.create();
                        configuration.set("hbase.zookeeper.quorum", CONFIGURATION.getConfig(ConfigurationKeys.STORE_HBASE_ZOOKEEPER_QUORUM));
//                        configuration.set("hbase.zookeeper.property.clientPort", CONFIGURATION.getConfig(ConfigurationKeys.STORE_HBASE_PROPERTY_CLIENTPORT));
                        configuration.set("hbase.client.ipc.pool.type", CONFIGURATION.getConfig(ConfigurationKeys.STORE_HBASE_POOL_TYPE,POOL_TYPE));
                        configuration.set("hbase.client.ipc.pool.size", CONFIGURATION.getConfig(ConfigurationKeys.STORE_HBASE_POOL_SIZE,POOL_SIZE));
                        connection = ConnectionFactory.createConnection(configuration);
                        LOGGER.info("the connection of HBase is created successfully.");
                    } catch (IOException e) {
                        LOGGER.error("the connection of HBase is created failed.", e.getMessage());
                        //改成HBase专有异常类
                    }
                }
            }
        }
        return connection;
    }
}


