package org.apache.seata.discovery.registry.raft;

import org.apache.seata.discovery.registry.RegistryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RaftRegistryProviderTest {
    @Test
    public void testProvide() {
        // Given
        RaftRegistryProvider provider = new RaftRegistryProvider();

        // When
        RegistryService<?> service = provider.provide();

        // Then
        assertNotNull(service);
        assertTrue(service instanceof RaftRegistryServiceImpl);

        // And getting it again should return the same instance
        RegistryService<?> service2 = provider.provide();
        assertSame(service, service2);
    }
}
