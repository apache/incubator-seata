package org.apache.seata.discovery.registry.eureka;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EurekaRegistryProviderTest {

    @Test
    void testProvide(){
        assertThat(new EurekaRegistryProvider()).isInstanceOf(EurekaRegistryProvider.class);
    }
}
