package org.apache.seata.spring.boot.autoconfigure.properties.registry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RegistryNamingServerPropertiesTest {

    @Test
    public void testRegistryNamingServerProperties() {
        RegistryNamingServerProperties namingServerProperties = new RegistryNamingServerProperties();
        namingServerProperties.setCluster("cluster");
        namingServerProperties.setServerAddr("addr");
        namingServerProperties.setNamespace("namespace");
        namingServerProperties.setHeartbeatPeriod(1);
        namingServerProperties.setMetadataMaxAgeMs(1L);
        namingServerProperties.setUsername("username");
        namingServerProperties.setPassword("password");
        namingServerProperties.setTokenValidityInMilliseconds(1L);

        Assertions.assertEquals("cluster", namingServerProperties.getCluster());
        Assertions.assertEquals("addr", namingServerProperties.getServerAddr());
        Assertions.assertEquals("namespace", namingServerProperties.getNamespace());
        Assertions.assertEquals(1, namingServerProperties.getHeartbeatPeriod());
        Assertions.assertEquals(1L, namingServerProperties.getMetadataMaxAgeMs());
        Assertions.assertEquals("username", namingServerProperties.getUsername());
        Assertions.assertEquals("password", namingServerProperties.getPassword());
        Assertions.assertEquals(1L, namingServerProperties.getTokenValidityInMilliseconds());
    }
}
