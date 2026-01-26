package org.apache.seata.discovery.registry.nacos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NacosRegistryProviderTest {
    @Test
    public void shouldReturnProviderInstance() {
        NacosRegistryProvider actualProvider = new NacosRegistryProvider();

        assertNotNull(actualProvider.provide());
        assertEquals(NacosRegistryServiceImpl.class, actualProvider.provide().getClass());
    }
}
