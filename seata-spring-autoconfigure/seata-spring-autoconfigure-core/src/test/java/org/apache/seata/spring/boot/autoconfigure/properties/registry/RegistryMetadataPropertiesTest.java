package org.apache.seata.spring.boot.autoconfigure.properties.registry;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RegistryMetadataPropertiesTest {

    @Test
    void testRegistryMetadataProperties() {
        RegistryMetadataProperties metadataProperties = new RegistryMetadataProperties();

        metadataProperties.setExternal("external-metadata");
        assertEquals("external-metadata", metadataProperties.getExternal());

        metadataProperties.setExternal(null);
        assertNull(metadataProperties.getExternal());
    }
}
