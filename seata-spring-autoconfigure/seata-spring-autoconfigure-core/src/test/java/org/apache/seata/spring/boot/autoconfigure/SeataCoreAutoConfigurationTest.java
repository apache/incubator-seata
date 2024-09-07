package org.apache.seata.spring.boot.autoconfigure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = SeataCoreAutoConfiguration.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class SeataCoreAutoConfigurationTest {

    @Autowired
    private Environment environment;

    @Test
    public void testSeataPropertiesLoaded() {
        // default file.conf
        String txServiceGroup = environment.getProperty("seata.store.db.url");
        assertEquals("jdbc:mysql://127.0.0.1:3306/seata?rewriteBatchedStatements=true&configType=file", txServiceGroup, "The transaction service group should be correctly loaded from configuration");

        // overridden by application-test.properties
        String registryType = environment.getProperty("seata.config.type");
        assertEquals("file", registryType, "The config type should be file");

        // overridden by application-test.properties
        String seataNamespaces = environment.getProperty("seata.config.nacos.namespace");
        assertEquals("seata-test-application.yml", seataNamespaces, "The config type should be file");
    }
}