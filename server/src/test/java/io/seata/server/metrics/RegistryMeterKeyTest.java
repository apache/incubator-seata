package io.seata.server.metrics;

import io.seata.metrics.Counter;
import io.seata.metrics.Id;
import io.seata.metrics.IdConstants;
import org.junit.jupiter.api.Test;

import static io.seata.server.metrics.MeterIdConstants.COUNTER_ACTIVE;

public class RegistryMeterKeyTest {
    @Test
    public void testGetIdMeterKey() {
        System.out.println(COUNTER_ACTIVE.getMeterKey());
        Id id = COUNTER_ACTIVE.withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM);
        System.out.println(id.getMeterKey());
    }

}
