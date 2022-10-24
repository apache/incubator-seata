/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
