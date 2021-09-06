package io.seata.config.processor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Properties;


class ConfigProcessorTest {

    @Test
    void loadConfig() throws IOException {
        String yamlString ="store:\n" +
                "  mode: db\n" +
                "  db: \n" +
                "    datasource: druid\n" +
                "    dbType: mysql\n" +
                "    driverClassName: com.mysql.jdbc.Driver\n" +
                "    url: jdbc:mysql://127.0.0.1:3306/server_seata\n" +
                "    user: root\n" +
                "    password: 'root'\n";

        final Properties properties = ConfigProcessor.loadConfig(yamlString,"yaml");
        Assertions.assertEquals(properties.getProperty("store.mode"),"db");

    }
}