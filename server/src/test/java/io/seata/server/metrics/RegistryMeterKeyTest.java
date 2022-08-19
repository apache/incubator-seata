package io.seata.server.metrics;

import io.seata.metrics.Id;
import io.seata.metrics.IdConstants;
import io.seata.metrics.Counter;
import io.seata.metrics.registry.RegistryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.seata.server.metrics.MeterIdConstants.COUNTER_ACTIVE;

public class RegistryMeterKeyTest {
    @Test
    public void testGetIdMeterKey() {
        Id id1 = COUNTER_ACTIVE.withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM);
        String meterKey1 = id1.getMeterKey();
        Id id2 = COUNTER_ACTIVE.withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC);
        String meterKey2 = id2.getMeterKey();
        Id id3 = COUNTER_ACTIVE.withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM);
        String meterKey3 = id3.getMeterKey();

        Assertions.assertNotEquals(meterKey2, meterKey1);
        Assertions.assertEquals(meterKey3, meterKey1);
    }

}
