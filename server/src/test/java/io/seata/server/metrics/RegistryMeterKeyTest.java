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
        Id id2 = COUNTER_ACTIVE.withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC);
        Id id3 = COUNTER_ACTIVE.withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM);

        Assertions.assertNotEquals(id1.getMeterKey(), id2.getMeterKey());
        Assertions.assertEquals(id1.getMeterKey(),id3.getMeterKey());

        Counter c1 = RegistryFactory.getInstance().getCounter(id1);
        Counter c2 = RegistryFactory.getInstance().getCounter(id2);
        Counter c3 = RegistryFactory.getInstance().getCounter(id3);

        Assertions.assertNotEquals(c1, c2);
        Assertions.assertEquals(c2, c3);

    }

}
